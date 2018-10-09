(ns stencil.api-test
  (:require [stencil.api :refer :all]))

(comment


  (def template-1 (prepare "/home/erdos/Joy/stencil/test-resources/test-control-conditionals.docx"))

  (defn render-template-1 [output-file data]
    (render! template-1 data :output output-file))

  (render-template-1 "/tmp/output-3.docx" {"customerName" "John Doe"})

  (def template-odt (prepare "/home/erdos/moby-aegon/templates/stencil/SZAMLA.odt"))

  ;; (.getSecretObject template-odt)

  (def szamla-data
    {"creationTime" "2018.10.08.",
     "vatRate" "ÁFA mentes",
     "sumNetAmount" "-107 579",
     "invoiceItems" [{"description" "Biztosítási szolgáltatás",
                      "quantity" 1,
                      "quantityUnit" "DB",
                      "netUnitPrice" "107 273", "netAmount" "102 579",
                      "vatRate" "0", "vatAmount" "0", "grossAmount" "101 579"}],
     "poNumber" nil, "sumGrossAmount" "-101 579", "totalAmount" "-101 579",
     "fileName" "Szamla-000000-2018_CS001146", "sumVatAmount" "0",
     "fulfillmentDate" "2018.10.01.", "totalAmountStr" "mínusz százhétezer-ötszázhetvenkilenc",
     "actualDate" "2018.10.08.",
     "addresseePartner" {"name" "Whatever.", "address" "HU 9000 Öttevény Rózsa"},
     "suspenseAmount" "0",
     "customerPartner" {"name" "Whatever Kft.", "address" "HU 9000  Öttevény Rózsa",
                        "taxNumber" "234234234-234234324", :policyRef "CS001146"},
     "swiftCode" "HUF", "dueDate" "2018.10.11.",
     "comment" "ACSBM-000609-2018 számú számla sztornója",
     "paymentMode" "Banki átutalás", "bonusAmount" "0",
     "supplierPartner" {"name" nil, "address" nil, "bankAccount" nil, "taxNumber" nil, "groupTaxNumber" nil},
     "invoiceRef" "ACSBM-000693-2018"})

  (render! template-odt szamla-data
           :output (clojure.java.io/file "/tmp/out5.odt")
           :overwrite? true)

  )
