package models.database

import models.database.FRM.{Coloc, CredSet, D2V2GScored, Genes, Overlaps, Studies, SumStatsGWAS, SumStatsMolTraits, V2DsByChrPos, V2DsByStudy, V2GOverallScore, V2GScored, V2GStructure, Variants}
import slick.lifted.TableQuery

trait GeneticsDbTables {

  lazy val genes = TableQuery[Genes]
  lazy val variants = TableQuery[Variants]
  lazy val studies = TableQuery[Studies]
  lazy val overlaps = TableQuery[Overlaps]
  lazy val v2gStructures = TableQuery[V2GStructure]
  lazy val v2DsByChrPos = TableQuery[V2DsByChrPos]
  lazy val v2DsByStudy = TableQuery[V2DsByStudy]
  lazy val v2gsScored = TableQuery[V2GScored]
  lazy val v2gScores = TableQuery[V2GOverallScore]
  lazy val d2v2gScored = TableQuery[D2V2GScored]
  lazy val sumstatsGWAS = TableQuery[SumStatsGWAS]
  lazy val sumstatsMolTraits = TableQuery[SumStatsMolTraits]
  lazy val colocs = TableQuery[Coloc]
  lazy val credsets = TableQuery[CredSet]

}
