package io.github.edadma.importer

import pprint._

object Main extends App {

  pprintln(
    Importer.importFromString(
      """cats
        |pointy: boolean  round: boolean  whiskers: boolean   cat: boolean
        |true             true            true                true
        |""".trim.stripMargin,
      doubleSpaces = true
    ))

}
