package xyz.hyperreal.importer

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import xyz.hyperreal.char_reader.CharReader

object Importer {

  private val converters = new mutable.HashMap[String, String => Option[AnyRef]]

  converters("integer") = { s =>
    if (s matches "-?\\d+") Some(s.toInt.asInstanceOf[Integer]) else None
  }
  converters("date") = DateConverter
  converters("float") = DecimalConverter
  converters("currency") = DecimalConverter
  converters("decimal") = DecimalConverter
  converters("timestamp") = TimestampConverter
  converters("uuid") = UUIDConverter

  def addConverter(name: String, converter: String => Option[AnyRef]): Unit = {
    converters(name) = converter
  }

  def importFromString(s: String, doubleSpaces: Boolean): Iterable[Table] =
    importFromReader(CharReader.fromString(s), doubleSpaces)

  def problem(msg: String, r: CharReader): Nothing = r.error(msg)

  def space(r: CharReader, e: String = "unexpected end of input"): CharReader =
    skipSpace(r) match {
      case rest if rest.eoi => problem(e, r)
      case rest             => rest
    }

  @scala.annotation.tailrec
  def skipSpace(r: CharReader): CharReader =
    if (!r.eoi && r.ch.isWhitespace && r.ch != '\n')
      skipSpace(r.next)
    else
      r

  @scala.annotation.tailrec
  def skipUntilNextLine(r: CharReader): CharReader =
    if (!r.eoi)
      if (r.ch == '\n')
        r.next
      else
        skipUntilNextLine(r.next)
    else
      r

  @scala.annotation.tailrec
  def skipEmptyLines(r: CharReader): CharReader = {
    val r1 = skipSpace(r)

    if (!r1.eoi && (r1.ch == '#' || r1.ch == '\n'))
      skipEmptyLines(skipUntilNextLine(r))
    else
      r1
  }

  def string(r: CharReader, doubleSpaces: Boolean): (CharReader, String) = {
    val buf = new StringBuilder

    @scala.annotation.tailrec
    def read(r: CharReader): CharReader =
      if (r.ch == '\\') {
        if (r.next.eoi)
          problem("unexpected end of input after escape character", r.next)
        else if (r.next.ch == 'u') {
          var r1 = r.next.next
          val ch = Array[Char](4)

          for (i <- 0 until 4)
            if (r1.eoi)
              problem("unexpected end of input within character code", r.next)
            else {
              val c = Character.toLowerCase(r1.ch)

              if ("0123456789abcdef".indexOf(c) == -1)
                problem("invalid character code", r1)

              ch(i) = c
              r1 = r1.next
            }

          buf += Integer.parseInt(ch mkString "", 16).toChar
          read(r1)
        } else {
          buf +=
            (r.next.ch match {
              case '"'  => '"'
              case '\\' => '\\'
              case '/'  => '/'
              case 'b'  => '\b'
              case 'f'  => '\f'
              case 'n'  => '\n'
              case 'r'  => '\r'
              case 't'  => '\t'
              case c    => problem(s"illegal escape character '$c'", r.next)
            })

          read(r.next.next)
        }
      } else if (r.eoi || r.ch == '\n' || r.ch == '\t' || doubleSpaces && r.ch.isWhitespace && (r.next.eoi || r.next.ch.isWhitespace))
        r
      else {
        buf += r.ch
        read(r.next)
      }

    (read(skipSpace(r)), buf.toString.trim)
  }

  def nl(r: CharReader): Boolean = r.ch == '\n'

  @scala.annotation.tailrec
  def skip(r: CharReader, n: Int): CharReader =
    if (n == 0) r else skip(r.next, n - 1)

  def table(r: CharReader, tables: mutable.LinkedHashMap[String, Table], doubleSpaces: Boolean): (CharReader, Table) = {
    val data = new ListBuffer[Vector[AnyRef]]
    val (r1, name) = string(r, doubleSpaces)

    if (tables contains name)
      problem("table with that name already exists", r)

    val header = new ArrayBuffer[Column]
    val r2 = skipSpace(r1)

    if (!nl(r2))
      problem("expected end of line after table name", r2)

    if (r2.eoi)
      problem("unexpected end of file after table name", r2)

    @scala.annotation.tailrec
    def columns(r: CharReader): CharReader = {
      val r0 = skipSpace(r)

      if (r0.eoi || r0.ch == '\n')
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
                    problem(s"no converter for this type ($t)", skip(r0, n.length + 1))

                  (n, t, a)
                case List(n) => (n, "text", a)
              }
          }

        header += Column(name, typ, args)
        columns(r1)
      }
    }

    val r3 = columns(r2.next)

    if (r3.eoi)
      problem("unexpected end of file after header", r3)

    if (header.isEmpty)
      problem("empty header", r2.next)

    @scala.annotation.tailrec
    def lines(r: CharReader): CharReader = {
      val values = new ArrayBuffer[AnyRef]

      @scala.annotation.tailrec
      def line(idx: Int, r: CharReader): CharReader = {
        val r0 = skipSpace(r)

        if (r0.eoi || r0.ch == '\n') {
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

      if (skipSpace(r).eoi || skipSpace(r).ch == '\n')
        skipSpace(r)
      else {
        val r1 = line(0, r)

        data += values.toVector
        lines(r1.next)
      }
    }

    val r4 = lines(r3.next)

    (r4, Table(name, header.toVector, data toList))
  }

  def importFromReader(r: CharReader, doubleSpaces: Boolean): Iterable[Table] = {
    val tables = mutable.LinkedHashMap.empty[String, Table]

    @scala.annotation.tailrec
    def read(r: CharReader): Unit = {
      val r1 = skipEmptyLines(r)

      if (!r1.eoi) {
        val (r2, tab) = table(r1, tables, doubleSpaces)

        tables += (tab.name -> tab)
        read(r2)
      }
    }

    read(r)
    tables.values
  }
}
