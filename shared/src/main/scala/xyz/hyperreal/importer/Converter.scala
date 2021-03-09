package xyz.hyperreal.importer

import xyz.hyperreal.datetime.{Datetime, DatetimeFormatter}

abstract class Converter[T] extends (String => Option[T])

object TimestampConverter extends Converter[Datetime] {
  def apply(date: String): Option[Datetime] = {
    try {
      Some(Datetime.fromString(date))
    } catch {
      case _: Exception => None
    }
  }
}

object DateConverter extends Converter[Datetime] {
  private val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""" r

  def apply(date: String): Option[Datetime] = {
    try {
      val dateRegex(y, m, d) = date

      Some(Datetime(y.toInt, m.toInt, d.toInt))
    } catch {
      case _: Exception => None
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
