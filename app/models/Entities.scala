package models

import slick.jdbc.GetResult

object Entities {
  case class Position(chr_id: String, position: Long)
  case class Variant(chr_id: String, position: Long, ref_allele: String, alt_allele: String)
  case class Gene(gene_chr: String, gene_id: String, gene_start: Long, gene_end: Long, gene_name: String)

  case class V2GEv(chr_id: String, position: Long, segment: Int, ref_allele: String, alt_allele: String, rs_id: String,
                   gene_chr: String, gene_id: String, gene_start: Long, gene_end: Long, gene_name: String,
                   feature: String, type_id: String, source_id: String, csq_counts: Option[Long],
                   qtl_beta: Option[Double], qtl_se: Option[Double], qtl_pval: Option[Double],
                   interval_score: Option[Double])
//  case class V2DEv(chr_id: String, position: Long, segment: Int, ref_allele: String, alt_allele: String, rs_id: String,
  //                   index_chr_id: String, index_position: Long, index_ref_allele: String, index_alt_allele: String, index_rs_id: String,
  //                   efo_code: String, efo_label: String, r2: Option[Double], afr: Option[Double], mar: Option[Double],
  //                   eas: Option[Double], eur: Option[Double], sas: Option[Double], log10_abf: Option[Double],
  //                   posterior_prob: Option[Double], pval: Option[Double], n_initial: Option[Int], n_replication: Option[Int],
  //                   trait_reported: Option[String], ancestry_initial: Option[String], ancestry_replication: Option[String],
  //                   pmid: Option[String], pub_date: Option[String], pub_journal: Option[String], pub_author: Option[String])

  // type ManhattanAssociation {
  //    indexVariantId: String!
  //    indexVariantRsId: String
  //    pval: Float!
  //    chromosome: String!
  //    position: Int!
  //    bestGenes: [Gene!]
  //    # could have index variant which has no tag variants (goes nowhere on click)
  //    credibleSetSize: Int
  //    ldSetSize: Int
  //    # TODO: get this
  //    # maf: Float
  //}

  case class SimpleGene(id: String, name: Option[String])
  case class ManhattanAssoc(indexVariantID: String, indexVariantRSID: Option[String], pval: Double,
                                    chromosome: String, position: Long, bestGenes: List[SimpleGene],
                                    crediblbeSetSize: Option[Int], ldSetSize: Option[Int])

  case class V2GRegionSummary(feature: String, avg_position: Long, uniq_genes: Long, uniq_variants: Long)
  implicit val getV2GRegionSummary = GetResult(r =>
    V2GRegionSummary(r.<<, r.<<, r.<<, r.<<))

  case class D2V2GRegionSummary(index_chr_id: String, index_position: Long, index_ref_allele: String,
                                index_alt_allele: String, uniq_genes: Long, uniq_tag_variants: Long,
                                count_evs: Long)
  implicit val getD2V2GRegionSummary = GetResult(r =>
    D2V2GRegionSummary(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  case class ManhattanRow(index_variant_id: String, index_rs_id: Option[String], index_chr_id: String,
                          index_position: Long, index_ref_allele: String, index_alt_allele: String, pval: Double)
  implicit val getManhattanRow = GetResult(r =>
    ManhattanRow(r.<<, r.<<?, r.<<, r.<<, r.<<, r.<<, r.<<))

}
