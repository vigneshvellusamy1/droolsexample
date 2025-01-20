package com.example.drools.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.drools.model.Loan;
import com.example.drools.service.LoanService;

@RestController
@RequestMapping("/api")
public class LoanController {
    
    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);
    
    @Autowired
    private LoanService loanService;
    
    @GetMapping("/loanpercent")
    public ResponseEntity<?> loanallocate(
            @RequestParam(required = true, name = "loanType") String loanType) {
        try {
            logger.info("Processing loan request for type: {}", loanType);
            
            Loan loanObj = new Loan();
            loanObj.setLoanType(loanType);
            
            Loan processedLoan = loanService.allocateLoan(loanObj);
            
            if (processedLoan == null) {
                logger.warn("No loan processing results returned for type: {}", loanType);
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body("No results found for loan type: " + loanType);
            }
            
            logger.info("Successfully processed loan request for type: {}", loanType);
            return ResponseEntity.ok(processedLoan);
            
        } catch (Exception e) {
            logger.error("Error processing loan request for type: {}", loanType, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing loan request: " + e.getMessage());
        }
    }
}