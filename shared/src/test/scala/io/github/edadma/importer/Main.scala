package io.github.edadma.importer

object Main extends App {

  println(
    Importer.importFromString(
      """
      |e: a, b, c
      |
      |x
      | id: integer, pk        a: timestamp        b: e 
      | 1                2020-12-09T17:19:55.137Z  null
      | 2                2020-12-09T18:19:55.137Z  null
      |""".trim.stripMargin,
      doubleSpaces = true
    ))

}
