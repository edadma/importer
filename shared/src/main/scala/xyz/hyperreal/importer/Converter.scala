package xyz.hyperreal.importer

import java.time.{Instant, LocalDate}
import java.time.format.{DateTimeFormatter, DateTimeParseException}

abstract class Converter[T] extends (String => Option[T])

object DateUSConverter extends Converter[LocalDate] {
  private val formatter = DateTimeFormatter.ofPattern("M/d/yyyy")

  def apply(date: String): Option[LocalDate] = {
    try {
      Some(LocalDate.parse(date, formatter))
    } catch {
      case _: DateTimeParseException => None
    }
  }
}

object TimestampConverter extends Converter[Instant] {
  def apply(date: String): Option[Instant] = {
    try {
      Some(Instant.parse(date))
    } catch {
      case _: DateTimeParseException => None
    }
  }
}

object DateConverter extends Converter[LocalDate] {
  private val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""" r

  def apply(date: String): Option[LocalDate] = {
    try {
      val dateRegex(y, m, d) = date

      Some(LocalDate.of(y.toInt, m.toInt, d.toInt))
      //      Some(LocalDate.parse(date))
    } catch {
      case _: DateTimeParseException => None
    }
  }
}

object DecimalConverter extends Converter[BigDecimal] {
  def apply(amount: String): Option[BigDecimal] =
    if (amount matches """-?\d+(:?\.\d\d?)?""")
      Some(BigDecimal(amount))
    else
      None
}
