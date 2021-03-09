package xyz.hyperreal.importer

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.util.parsing.input.{CharSequenceReader, Reader}

object Importer {

  private val converters = new mutable.HashMap[String, String => Option[AnyRef]]

  converters("integer") = { s =>
    if (s matches "-?\\d+") Some(s.toInt.asInstanceOf[Integer]) else None
  }
  converters("dateUS") = DateUSConverter
  converters("date") = DateConverter
  converters("currency") = DecimalConverter
  converters("decimal") = DecimalConverter
  converters("timestamp") = TimestampConverter

  def addConverter(name: String, converter: String => Option[AnyRef]): Unit = {
    converters(name) = converter
  }

  def importFromString(s: String, doubleSpaces: Boolean): Iterable[Table] =
    importFromReader(new CharSequenceReader(s), doubleSpaces)

  def problem(msg: String, r: Reader[Char]): Nothing =
    sys.error(msg + " at " + r.pos + "\n" + r.pos.longString)

  def space(r: Reader[Char],
            e: String = "unexpected end of input"): Reader[Char] =
    skipSpace(r) match {
      case rest if rest.atEnd => problem(e, r)
      case rest               => rest
    }

  @scala.annotation.tailrec
  def skipSpace(r: Reader[Char]): Reader[Char] =
    if (!r.atEnd && r.first.isWhitespace && r.first != '\n')
      skipSpace(r.rest)
    else
      r

  @scala.annotation.tailrec
  def skipUntilNextLine(r: Reader[Char]): Reader[Char] =
    if (!r.atEnd)
      if (r.first == '\n')
        r.rest
      else
        skipUntilNextLine(r.rest)
    else
      r

  @scala.annotation.tailrec
  def skipEmptyLines(r: Reader[Char]): Reader[Char] = {
    val r1 = skipSpace(r)

    if (!r1.atEnd && (r1.first == '#' || r1.first == '\n'))
      skipEmptyLines(skipUntilNextLine(r))
    else
      r1
  }

  def string(r: Reader[Char], doubleSpaces: Boolean): (Reader[Char], String) = {
    val buf = new StringBuilder

    @scala.annotation.tailrec
    def read(r: Reader[Char]): Reader[Char] =
      if (r.first == '\\') {
        if (r.rest.atEnd)
          problem("unexpected end of input after escape character", r.rest)
        else if (r.rest.first == 'u') {
          var r1 = r.rest.rest
          val ch = Array[Char](4)

          for (i <- 0 until 4)
            if (r1.atEnd)
              problem("unexpected end of input within character code", r.rest)
            else {
              val c = Character.toLowerCase(r1.first)

              if ("0123456789abcdef".indexOf(c) == -1)
                problem("invalid character code", r1)

              ch(i) = c
              r1 = r1.rest
            }

          buf += Integer.parseInt(ch mkString "", 16).toChar
          read(r1)
        } else {
          buf +=
            (r.rest.first match {
              case '"'  => '"'
              case '\\' => '\\'
              case '/'  => '/'
              case 'b'  => '\b'
              case 'f'  => '\f'
              case 'n'  => '\n'
              case 'r'  => '\r'
              case 't'  => '\t'
              case c    => problem(s"illegal escape character '$c'", r.rest)
            })

          read(r.rest.rest)
        }
      } else if (r.atEnd || r.first == '\n' || r.first == '\t' || doubleSpaces && r.first.isWhitespace && (r.rest.atEnd || r.rest.first.isWhitespace))
        r
      else {
        buf += r.first
        read(r.rest)
      }

    (read(skipSpace(r)), buf.toString.trim)
  }

  def nl(r: Reader[Char]): Boolean = r.first == '\n'

  @scala.annotation.tailrec
  def skip(r: Reader[Char], n: Int): Reader[Char] =
    if (n == 0) r else skip(r.rest, n - 1)

  def table(r: Reader[Char],
            tables: mutable.LinkedHashMap[String, Table],
            doubleSpaces: Boolean): (Reader[Char], Table) = {
    val data = new ListBuffer[Vector[AnyRef]]
    val (r1, name) = string(r, doubleSpaces)

    if (tables contains name)
      problem("table with that name already exists", r)

    val header = new ArrayBuffer[Column]
    val r2 = skipSpace(r1)

    if (!nl(r2))
      problem("expected end of line after table name", r2)

    if (r2.atEnd)
      problem("unexpected end of file after table name", r2)

    @scala.annotation.tailrec
    def columns(r: Reader[Char]): Reader[Char] = {
      val r0 = skipSpace(r)

      if (r0.atEnd || r0.first == '\n')
        r0
      else {
        val (r1, c) = string(r0, doubleSpaces)
        val (name, typ, args) =
          c.split(" *, *").toList match {
            case Nil | "" :: _ => problem("invalid header", r0)
            case nt :: a =>
              nt.split(" *: *", 2).toList match {
                case List(n, t) =>
                  if (!converters.contains(t) && t != "text")
                    problem(s"no converter for this type ($t)",
                            skip(r0, n.length + 1))

                  (n, t, a)
                case List(n) => (n, "text", a)
              }
          }

        header += Column(name, typ, args)
        columns(r1)
      }
    }

    val r3 = columns(r2.rest)

    if (r3.atEnd)
      problem("unexpected end of file after header", r3)

    if (header isEmpty)
      problem("empty header", r2.rest)

    @scala.annotation.tailrec
    def lines(r: Reader[Char]): Reader[Char] = {
      val values = new ArrayBuffer[AnyRef]

      @scala.annotation.tailrec
      def line(idx: Int, r: Reader[Char]): Reader[Char] = {
        val r0 = skipSpace(r)

        if (r0.atEnd || r0.first == '\n') {
          if (idx < header.length - 1)
            problem("not enough values in this row", r0)

          r0
        } else {
          if (idx == header.length)
            problem("too many values in this row", r0)

          val (r1, s) = string(r0, doubleSpaces)
          val s1 =
            if (s.toLowerCase == "null")
              null
            else if (header(idx).typ == "text")
              s
            else
              converters(header(idx).typ)(s) match {
                case None =>
                  problem(s"error converting ${header(idx).typ} value", r0)
                case Some(v) => v
              }
          values += s1
          line(idx + 1, r1)
        }
      }

      if (skipSpace(r).atEnd || skipSpace(r).first == '\n')
        skipSpace(r)
      else {
        val r1 = line(0, r)

        data += values.toVector
        lines(r1.rest)
      }
    }

    val r4 = lines(r3.rest)

    (r4, Table(name, header toVector, data toList))
  }

  def importFromReader(r: Reader[Char],
                       doubleSpaces: Boolean): Iterable[Table] = {
    val tables = mutable.LinkedHashMap.empty[String, Table]

    @scala.annotation.tailrec
    def read(r: Reader[Char]): Unit = {
      val r1 = skipEmptyLines(r)

      if (!r1.atEnd) {
        val (r2, tab) = table(r1, tables, doubleSpaces)

        tables += (tab.name -> tab)
        read(r2)
      }
    }

    read(r)
    tables.values
  }
}
