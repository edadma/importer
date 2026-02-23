# importer

![Maven Central](https://img.shields.io/maven-central/v/io.github.edadma/importer_sjs1_3)
[![Last Commit](https://img.shields.io/github/last-commit/edadma/importer)](https://github.com/edadma/importer/commits)
![GitHub](https://img.shields.io/github/license/edadma/importer)
![Scala Version](https://img.shields.io/badge/Scala-3.8.1-blue.svg)
![ScalaJS Version](https://img.shields.io/badge/Scala.js-1.20.2-blue.svg)
![Scala Native Version](https://img.shields.io/badge/Scala_Native-0.5.10-blue.svg)

A cross-platform Scala 3 library for importing typed tabular data from a simple, human-readable text format. Supports JVM, Scala.js (Node.js), and Scala Native.

## Overview

`importer` parses a lightweight whitespace-delimited text format into structured tables with typed columns. Headers declare column names and types; the library automatically converts each cell value to the appropriate Scala type. Supported types include integers, floats, decimals, currencies, booleans, dates, timestamps, UUIDs, and arbitrary text. Custom enum types and user-defined converters are also supported.

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.edadma" %%% "importer" % "0.1.0"
```

## Data Format

Data files consist of one or more table blocks. Each block starts with a table name, followed by a header row of typed column definitions, then data rows:

```
users
id:integer  name  email           active:boolean  joined:date
1           Alice alice@example.com  true          2023-01-15
2           Bob   bob@example.com    false         2023-03-22
```

Column headers use the syntax `name:type`. Omitting the type defaults to `text`. Multiple column args are separated by commas.

### Supported Types

| Type        | Scala type        | Example value         |
|-------------|-------------------|-----------------------|
| `text`      | `String`          | `hello world`         |
| `integer`   | `Int`             | `42`                  |
| `float`     | `Double`          | `3.14`                |
| `bigint`    | `BigInt`          | `123456789012345`     |
| `decimal`   | `BigDecimal`      | `9.99`                |
| `currency`  | `BigDecimal`      | `19.99`               |
| `boolean`   | `Boolean`         | `true`, `yes`, `1`    |
| `date`      | `LocalDate`       | `2024-06-01`          |
| `timestamp` | `Instant`         | `2024-06-01T12:00:00Z`|
| `uuid`      | `String`          | `550e8400-e29b-41d4...`|

The special value `null` in any cell produces a `null` value regardless of column type.

### Enums

Declare named enum types before the tables that use them:

```
status: active, inactive, pending

orders
id:integer  status  amount:currency
1           active  19.99
2           pending  5.00
```

### Double-space mode

When `doubleSpaces = true`, two or more consecutive spaces act as a column delimiter, allowing single spaces within values:

```
products
name                    price:currency
Widget Deluxe           9.99
Super Gadget Pro Max    49.99
```

## Usage

```scala
import io.github.edadma.importer.Importer

// From a string
val result = Importer.importFromString(data, doubleSpaces = false)

// From a file
val result = Importer.importFromFile("data.txt", doubleSpaces = false)

// Access results
result.tables.foreach { table =>
  println(s"Table: ${table.name}")
  table.header.foreach(col => print(s"  ${col.name}:${col.typ}"))
  println()
  table.data.foreach(row => println(s"  $row"))
}

result.enums.foreach { e =>
  println(s"Enum ${e.name}: ${e.labels.mkString(", ")}")
}
```

### Adding a custom converter

```scala
Importer.addConverter("percent", s =>
  if s.endsWith("%") then s.dropRight(1).toDoubleOption else None
)
```

## Building and Testing

Requirements: JDK 11+, sbt 1.12.1+, Node.js (for JS), LLVM/Clang (for Native)

```bash
# Compile all platforms
sbt compile

# Run tests on all platforms
sbt test

# Run tests on a specific platform
sbt "importerJVM/test"
sbt "importerJS/test"
sbt "importerNative/test"
```

## License

ISC — see [LICENSE](LICENSE)
