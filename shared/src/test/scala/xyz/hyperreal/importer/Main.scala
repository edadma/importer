package xyz.hyperreal.importer

object Main extends App {

  println(
    Importer.importFromString(
      """
      |x
      | id: integer, pk        a: timestamp        b: timestamp 
      | 1                2020-12-09T17:19:55.137Z  null         
      | 2                2020-12-09T18:19:55.137Z  null         
      | 3                2020-12-09T16:19:55.137Z  null         
      |""".trim.stripMargin,
      doubleSpaces = true
    ))

}
