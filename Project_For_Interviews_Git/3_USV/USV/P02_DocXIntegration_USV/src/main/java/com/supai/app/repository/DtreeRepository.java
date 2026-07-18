//package com.supai.app.repository;
//
//import java.util.List;
//
//import org.springframework.stereotype.Repository;
//import org.springframework.transaction.annotation.Transactional;
//
//import jakarta.persistence.EntityManager;
//import jakarta.persistence.PersistenceContext;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Repository
//public class DtreeRepository {
//
//    @PersistenceContext
//    private EntityManager entityManager;
//
//    @Transactional(readOnly = true)
//    public String getParentIdByDataId(String dataId) {
//    	String subtype = "31067";
//        String query = """
//            SELECT dt.dataid
//            FROM dtreeancestors da
//            JOIN dtreecore dt ON dt.dataid = da.ancestorid
//            WHERE da.dataid = :dataId
//              AND dt.subtype = :subtype
//        """;
//
//        try {
////           log.info("Executing query with dataId: {}, subtype: {}", dataId, subtype);
//            List<?> results = entityManager.createNativeQuery(query)
//                    .setParameter("dataId", Long.parseLong(dataId))      // use Long since column: NUMBER
//                    .setParameter("subtype", Long.parseLong(subtype))    // use Long since column: NUMBER
//                    .getResultList();
//
//            if (results.isEmpty()) {
//                log.warn("No parentId found in DB for dataId={} and subtype={}", dataId, subtype);
//                return null;
//            }
//
//            String parentId = results.get(0).toString();
////            log.info("Found parentId: {}", parentId);
//            return parentId;
//
//        } catch (Exception e) {
//           log.error("Exception while executing query: {}", e.getMessage());
//            return null;
//        }
//    }
//}
