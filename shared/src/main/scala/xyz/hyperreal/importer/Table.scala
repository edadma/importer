package xyz.hyperreal.importer

case class Table(name: String,
                 header: Vector[Column],
                 data: List[Vector[AnyRef]])
