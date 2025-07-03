package io.github.edadma.importer

import pprint._

@main def run(): Unit =
  pprintln(
    Importer.importFromString(
      """cats
        |pointy: boolean  round: boolean  whiskers: boolean   cat: boolean
        |true             true            true                true
        |""".trim.stripMargin,
      doubleSpaces = true,
    ),
  )
