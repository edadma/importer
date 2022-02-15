package io.github.edadma.importer

import io.github.edadma.datetime.Datetime

abstract class Converter[T] extends (String => Option[T])

object TimestampConverter extends Converter[Datetime] {
  def apply(date: String): Option[Datetime] = {
    try {
      Some(Datetime.fromString(date).timestamp)
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

object UUIDConverter extends Converter[String] {
  def apply(amount: String): Option[String] =
    if (amount matches """(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")
      Some(amount)
    else
      None
}
