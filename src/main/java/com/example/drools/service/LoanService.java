package com.example.drools.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.RuleServicesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.kie.api.command.BatchExecutionCommand;
import org.kie.api.command.Command;
import org.kie.api.KieServices;
import org.kie.api.command.KieCommands;
import org.kie.server.api.model.ServiceResponse;
import org.kie.api.runtime.ExecutionResults;
import org.kie.internal.command.CommandFactory;

import com.example.drools.model.Loan;
import java.util.*;

@Service
public class LoanService {
    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    @Value("${kie.containerId}")
    private String containerId;

    @Value("${kie.server.user}")
    private String user;

    @Value("${kie.server.pwd}")
    private String password;

    @Value("${kie.server.url}")
    private String url;

    private static final String OUT_IDENTIFIER = "response";

    public Loan allocateLoan(Loan loanObj) {
        try {
            logger.info("Starting loan allocation for type: {} at URL: {}", loanObj.getLoanType(), url);

            // Create and configure KIE server configuration
            KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(url, user, password);
            
            // Set extended timeout
            config.setTimeout(300000); // 5 minutes
            
            // Set marshalling format
            config.setMarshallingFormat(MarshallingFormat.JSON);
            
            // Add capabilities
            Set<Class<?>> extraClasses = new HashSet<>();
            extraClasses.add(Loan.class);
            config.addExtraClasses(extraClasses);

            logger.debug("Configuration created with timeout: {}, format: {}", config.getTimeout(), config.getMarshallingFormat());

            // Create KIE services client
            logger.debug("Creating KIE services client...");
            KieServicesClient kieServicesClient = KieServicesFactory.newKieServicesClient(config);

            // Get rule services client
            logger.debug("Getting rule services client...");
            RuleServicesClient ruleClient = kieServicesClient.getServicesClient(RuleServicesClient.class);

            // Create commands
            List<Command<?>> commands = new ArrayList<>();
            KieCommands kieCommands = KieServices.Factory.get().getCommands();
            
            // Insert fact and fire rules
            commands.add(kieCommands.newInsert(loanObj, OUT_IDENTIFIER));
            commands.add(kieCommands.newFireAllRules());
            
            BatchExecutionCommand batchCommand = CommandFactory.newBatchExecution(commands);

            logger.info("Executing rules on container: {}", containerId);
            ServiceResponse<ExecutionResults> response = ruleClient.executeCommandsWithResults(containerId, batchCommand);

            if (response != null) {
                logger.debug("Response received: {}", response.getType());
                if (response.getType() == ServiceResponse.ResponseType.SUCCESS) {
                    logger.info("Rules executed successfully");
                    return (Loan) response.getResult().getValue(OUT_IDENTIFIER);
                } else {
                    String errorMessage = "Rule execution failed: " + response.getMsg();
                    logger.error(errorMessage);
                    throw new RuntimeException(errorMessage);
                }
            } else {
                throw new RuntimeException("Null response received from KIE server");
            }

        } catch (Exception e) {
            logger.error("Error in loan allocation: ", e);
            throw new RuntimeException("Failed to process loan allocation: " + e.getMessage(), e);
        }
    }
}