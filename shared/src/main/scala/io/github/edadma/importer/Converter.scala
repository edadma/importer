package io.github.edadma.importer

import java.time.{Instant, LocalDate, ZoneOffset, ZonedDateTime, LocalDateTime}

import scala.util.{Try, Success, Failure}

abstract class Converter[T] extends (String => Option[T])

object TimestampConverter extends Converter[Instant]:
  def apply(date: String): Option[Instant] =
    Try(ZonedDateTime.parse(date)) match
      case Success(t) => Some(t.toInstant)
      case _ =>
        Try(LocalDateTime.parse(date)) match
          case Success(t) => Some(t.toInstant(ZoneOffset.UTC))
          case _          => None

object DateConverter extends Converter[LocalDate]:
  private val dateRegex = """(\d\d\d\d)-(\d\d)-(\d\d)""".r

  def apply(date: String): Option[LocalDate] = {
    try {
      val dateRegex(y, m, d) = date

      Some(LocalDate.of(y.toInt, m.toInt, d.toInt))
    } catch {
      case _: Exception => None
    }
  }

object CurrencyConverter extends Converter[BigDecimal]:
  private val regex = """-?\d+\.\d\d""".r

  def apply(amount: String): Option[BigDecimal] =
    if (regex matches amount)
      Some(BigDecimal(amount))
    else
      None

object DecimalConverter extends Converter[BigDecimal]:
  private val regex = """-?\d+(:?\.\d+)?""".r

  def apply(amount: String): Option[BigDecimal] =
    if (regex matches amount)
      Some(BigDecimal(amount))
    else
      None

object BigIntConverter extends Converter[BigInt]:
  private val regex = """-?\d+""".r

  def apply(amount: String): Option[BigInt] = if regex matches amount then Some(BigInt(amount)) else None

object UUIDConverter extends Converter[String]:
  private val regex = """(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""".r

  def apply(s: String): Option[String] =
    if (regex matches s)
      Some(s)
    else
      None

object BooleanConverter extends Converter[Boolean]:
  private val t = Set("1", "true", "t", "True", "T", "yes", "y", "Yes", "Y", "on", "On")
  private val f = Set("0", "false", "f", "False", "F", "no", "n", "No", "N", "off", "Off")

  def apply(v: String): Option[Boolean] =
    if t(v) then Some(true)
    else if f(v) then Some(false)
    else None
