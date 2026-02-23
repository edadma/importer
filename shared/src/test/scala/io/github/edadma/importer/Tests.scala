package io.github.edadma.importer

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Instant, LocalDate}

class Tests extends AnyFreeSpec with Matchers {

  // double-space mode is most readable for test data
  def imp(s: String, doubleSpaces: Boolean = true): Import =
    Importer.importFromString(s.stripMargin.trim, doubleSpaces)

  "basic text table" in {
    val result = imp("""
      |people
      |name   city
      |Alice  London
      |Bob    Paris
      """)

    result.tables.length shouldBe 1

    val t = result.tables.head

    t.name shouldBe "people"
    t.header shouldBe Vector(Column("name", "text", Nil), Column("city", "text", Nil))
    t.data shouldBe List(
      Vector("Alice", "London"),
      Vector("Bob", "Paris"),
    )
  }

  "integer column" in {
    val result = imp("""
      |scores
      |name   score:integer
      |Alice  42
      |Bob    7
      """)

    result.tables.head.data shouldBe List(
      Vector("Alice", 42),
      Vector("Bob", 7),
    )
  }

  "float column" in {
    val result = imp("""
      |readings
      |label  value:float
      |temp   98.6
      |pi     3.14159
      """)

    val data = result.tables.head.data

    data(0)(1).asInstanceOf[Double] shouldBe 98.6 +- 0.001
    data(1)(1).asInstanceOf[Double] shouldBe 3.14159 +- 0.00001
  }

  "boolean column" in {
    val result = imp("""
      |flags
      |label  value:boolean
      |a      true
      |b      false
      |c      yes
      |d      no
      |e      1
      |f      0
      |g      t
      |h      f
      """)

    result.tables.head.data.map(_(1)) shouldBe List(true, false, true, false, true, false, true, false)
  }

  "date column" in {
    val result = imp("""
      |events
      |name    date:date
      |launch  2024-06-01
      |end     2024-12-31
      """)

    result.tables.head.data shouldBe List(
      Vector("launch", LocalDate.of(2024, 6, 1)),
      Vector("end", LocalDate.of(2024, 12, 31)),
    )
  }

  "timestamp with timezone" in {
    val result = imp("""
      |log
      |event    ts:timestamp
      |created  2024-06-01T12:00:00Z
      """)

    result.tables.head.data.head(1) shouldBe Instant.parse("2024-06-01T12:00:00Z")
  }

  "timestamp without timezone" in {
    val result = imp("""
      |log
      |event  ts:timestamp
      |start  2024-06-01T09:30:00
      """)

    result.tables.head.data.head(1) shouldBe Instant.parse("2024-06-01T09:30:00Z")
  }

  "currency column" in {
    val result = imp("""
      |prices
      |item    price:currency
      |widget  9.99
      |gadget  -14.50
      """)

    result.tables.head.data shouldBe List(
      Vector("widget", BigDecimal("9.99")),
      Vector("gadget", BigDecimal("-14.50")),
    )
  }

  "decimal column" in {
    val result = imp("""
      |measures
      |name  value:decimal
      |pi    3.14159265
      |neg   -0.5
      |int   42
      """)

    result.tables.head.data shouldBe List(
      Vector("pi", BigDecimal("3.14159265")),
      Vector("neg", BigDecimal("-0.5")),
      Vector("int", BigDecimal("42")),
    )
  }

  "bigint column" in {
    val result = imp("""
      |big
      |name  value:bigint
      |huge  123456789012345678901234567890
      |neg   -42
      """)

    result.tables.head.data shouldBe List(
      Vector("huge", BigInt("123456789012345678901234567890")),
      Vector("neg", BigInt("-42")),
    )
  }

  "uuid column" in {
    val result = imp("""
      |records
      |id:uuid
      |550e8400-e29b-41d4-a716-446655440000
      """)

    result.tables.head.data.head(0) shouldBe "550e8400-e29b-41d4-a716-446655440000"
  }

  "null values" in {
    val result = imp("""
      |data
      |name   score:integer
      |Alice  null
      |null   99
      """)

    result.tables.head.data shouldBe List(
      Vector("Alice", null),
      Vector(null, 99),
    )
  }

  "enum type" in {
    val result = imp("""
      |status: active, inactive, pending
      |
      |orders
      |id:integer  status:status
      |1           active
      |2           pending
      |3           inactive
      """)

    result.enums shouldBe List(Enum("status", List("active", "inactive", "pending")))
    result.tables.head.data shouldBe List(
      Vector(1, "active"),
      Vector(2, "pending"),
      Vector(3, "inactive"),
    )
  }

  "multiple tables" in {
    val result = imp("""
      |cats
      |name   color
      |Mimi   black
      |Luna   white
      |
      |dogs
      |name  breed
      |Rex   labrador
      """)

    result.tables.map(_.name) shouldBe List("cats", "dogs")
    result.tables(0).data.length shouldBe 2
    result.tables(1).data.length shouldBe 1
  }

  "comments and blank lines between tables" in {
    val result = imp("""
      |# first comment
      |
      |things
      |name   value:integer
      |alpha  1
      |beta   2
      |
      |# second comment
      |more
      |x  y
      |a  b
      """)

    result.tables.map(_.name) shouldBe List("things", "more")
    result.tables(0).data shouldBe List(Vector("alpha", 1), Vector("beta", 2))
  }

  "escape sequences" in {
    val result = imp("""
      |strings
      |value
      |before\nafter
      |say\"hi\"
      |back\\slash
      """)

    val values = result.tables.head.data.map(_(0))

    values(0) shouldBe "before\nafter"
    values(1) shouldBe "say\"hi\""
    values(2) shouldBe "back\\slash"
  }

  "unicode escape" in {
    val result = imp("""
      |chars
      |value
      |\u0041\u0042\u0043
      """)

    result.tables.head.data.head(0) shouldBe "ABC"
  }

  "tab-delimited mode" in {
    val data = "items\nname\tcount:integer\napples\t5\nbananas\t3"
    val result = Importer.importFromString(data, doubleSpaces = false)

    result.tables.head.data shouldBe List(
      Vector("apples", 5),
      Vector("bananas", 3),
    )
  }

  "double-space mode allows spaces within values" in {
    val result = imp(
      """
      |products
      |name                   price:currency
      |Widget Deluxe          9.99
      |Super Gadget Pro Max   49.99
      """,
      doubleSpaces = true,
    )

    result.tables.head.data shouldBe List(
      Vector("Widget Deluxe", BigDecimal("9.99")),
      Vector("Super Gadget Pro Max", BigDecimal("49.99")),
    )
  }

  "custom converter" in {
    Importer.addConverter("percent", s => if s.endsWith("%") then s.dropRight(1).toDoubleOption.map(Double.box) else None)

    val result = imp("""
      |stats
      |name  rate:percent
      |win   75%
      |loss  25%
      """)

    result.tables.head.data shouldBe List(
      Vector("win", 75.0),
      Vector("loss", 25.0),
    )
  }

  "error: unknown type" in {
    an[Exception] should be thrownBy imp("""
      |t
      |x:badtype
      |foo
      """)
  }

  "error: too many values" in {
    an[Exception] should be thrownBy imp("""
      |t
      |a  b
      |1  2  3
      """)
  }

  "error: not enough values" in {
    // last column may be omitted; error fires when more than the last col is missing
    an[Exception] should be thrownBy imp("""
      |t
      |a  b  c
      |1
      """)
  }

  "error: duplicate table name" in {
    an[Exception] should be thrownBy imp("""
      |t
      |x
      |1
      |
      |t
      |x
      |2
      """)
  }

  "error: bad integer value" in {
    an[Exception] should be thrownBy imp("""
      |t
      |x:integer
      |notanint
      """)
  }

}
