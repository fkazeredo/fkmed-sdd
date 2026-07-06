package com.fkmed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * SPEC-0011 §Persistence: the V18 seed — a representative set of documents across all 4 types,
 * bound to the seeded MARIA/PEDRO beneficiaries, with both expired and non-expired examples (BR5).
 */
class ClinicalDocumentSeedIT extends AbstractIntegrationTest {

  private static final String MARIA_ID = "3f2a1b4c-6d5e-4f7a-8b9c-0d1e2f3a4b5c";
  private static final String PEDRO_ID = "9c8b7a6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d";

  @Autowired private JdbcTemplate jdbc;

  @Test
  void seedsAllFourTypes_forBothMariaAndPedro() {
    for (String type : new String[] {"EXAM_ORDER", "REFERRAL", "PRESCRIPTION", "SICK_NOTE"}) {
      Long count =
          jdbc.queryForObject(
              "select count(*) from clinical_document where type = ? and beneficiary_id in"
                  + " (?::uuid, ?::uuid)",
              Long.class,
              type,
              MARIA_ID,
              PEDRO_ID);
      assertThat(count).as("seeded %s documents", type).isPositive();
    }
  }

  @Test
  void seedsAtLeastOneExpiredAndOneValidDocument_br5() {
    Long expired =
        jdbc.queryForObject(
            "select count(*) from clinical_document"
                + " where valid_until is not null and valid_until < current_date",
            Long.class);
    assertThat(expired).as("at least one expired seeded document").isPositive();

    Long valid =
        jdbc.queryForObject(
            "select count(*) from clinical_document"
                + " where valid_until is null or valid_until >= current_date",
            Long.class);
    assertThat(valid).as("at least one non-expired seeded document").isPositive();
  }

  @Test
  void seedsExamOrderItems_withTussCodes() {
    Long items =
        jdbc.queryForObject(
            "select count(*) from exam_order_item i"
                + " join clinical_document d on d.id = i.document_id"
                + " where d.beneficiary_id in (?::uuid, ?::uuid) and i.tuss_code is not null",
            Long.class,
            MARIA_ID,
            PEDRO_ID);
    assertThat(items).isPositive();
  }

  @Test
  void seedsPrescriptionItems() {
    Long items =
        jdbc.queryForObject(
            "select count(*) from prescription_item i"
                + " join clinical_document d on d.id = i.document_id"
                + " where d.beneficiary_id in (?::uuid, ?::uuid)",
            Long.class,
            MARIA_ID,
            PEDRO_ID);
    assertThat(items).isPositive();
  }

  @Test
  void seedsSickNoteWithCid_dl0020() {
    Long withCid =
        jdbc.queryForObject(
            "select count(*) from clinical_document where type = 'SICK_NOTE' and cid is not null"
                + " and beneficiary_id in (?::uuid, ?::uuid)",
            Long.class,
            MARIA_ID,
            PEDRO_ID);
    assertThat(withCid).isPositive();
  }

  @Test
  void everyDocument_hasExactlyOneOrigin() {
    Long invalid =
        jdbc.queryForObject(
            "select count(*) from clinical_document"
                + " where (origin_session_id is null) = (origin_operator_id is null)",
            Long.class);
    assertThat(invalid).isZero();
  }
}
