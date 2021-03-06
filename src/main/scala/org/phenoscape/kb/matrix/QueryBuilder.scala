package org.phenoscape.kb.matrix

import scala.language.postfixOps

import org.apache.jena.query.Query
import org.apache.jena.sparql.syntax.ElementFilter
import org.phenoscape.owl.NamedRestrictionGenerator
import org.phenoscape.owl.Vocab
import org.phenoscape.owl.Vocab._
import org.phenoscape.owlet.Owlet
import org.phenoscape.owlet.SPARQLComposer._
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.OWLClassExpression
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary

class QueryBuilder(owlReasoner: OWLReasoner) {

  implicit val reasoner = owlReasoner
  val factory = OWLManager.getOWLDataFactory
  val rdfsSubClassOf = factory.getOWLObjectProperty(OWLRDFVocabulary.RDFS_SUBCLASS_OF.getIRI)
  val implies_presence_of_some = NamedRestrictionGenerator.getClassRelationIRI(IMPLIES_PRESENCE_OF.getIRI)

  def absenceQuery(anatomicalTerms: TermScope, taxonomicTerms: TermScope): Query = {
    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label, 'curated_entity, 'curated_quality) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf*, 'absence),
        t('eq, Vocab.entity_term, 'curated_entity),
        t('eq, Vocab.quality_term, 'curated_quality),
        t('absence, ABSENCE_OF, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, describes_phenotype, 'eq),
        t('state, dcDescription, 'state_label),
        t('taxon, exhibits_state, 'state),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, has_character, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, may_have_state_value, 'state)),
        makeFilter('entity, anatomicalTerms),
        makeFilter('taxon, taxonomicTerms))
  }

  def presenceQuery(anatomicalTerms: TermScope, taxonomicTerms: TermScope): Query =
    select_distinct('entity, 'entity_label, 'taxon, 'taxon_label, 'state, 'state_label, 'matrix_label, 'curated_entity, 'curated_quality) from "http://kb.phenoscape.org/" where (
      bgp(
        t('eq, rdfsSubClassOf*, 'presence),
        t('eq, Vocab.entity_term, 'curated_entity),
        t('eq, Vocab.quality_term, 'curated_quality),
        t('presence, implies_presence_of_some, 'entity),
        t('entity, rdfsLabel, 'entity_label),
        t('state, describes_phenotype, 'eq),
        t('state, dcDescription, 'state_label),
        t('taxon, exhibits_state, 'state),
        t('taxon, rdfsLabel, 'taxon_label),
        t('matrix, has_character, 'matrix_char),
        t('matrix, rdfsLabel, 'matrix_label),
        t('matrix_char, may_have_state_value, 'state)),
        makeFilter('entity, anatomicalTerms),
        makeFilter('taxon, taxonomicTerms))

  private def makeFilter(variable: Symbol, terms: TermScope): ElementFilter = terms match {
    case LogicalScope(classExpression) => subClassOf(variable, classExpression)
    case ListScope(terms)              => Owlet.makeFilter(variable, terms)
  }

}

sealed trait TermScope

final case class LogicalScope(classExpression: OWLClassExpression) extends TermScope {

  override def toString(): String = s"Logical scope: $classExpression"

}

final case class ListScope(terms: Iterable[OWLClass]) extends TermScope {

  override def toString(): String = s"List scope: ${terms.mkString(", ")}"

}
