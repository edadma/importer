package io.github.edadma.importer

case class Import(enums: List[Enum], tables: List[Table])

case class Enum(name: String, labels: List[String])

case class Table(name: String, header: Vector[Column], data: List[Vector[Any]])
