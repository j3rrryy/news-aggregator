package dev.j3rrryy.news_aggregator.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

public class CustomPostgreSQLDialect extends PostgreSQLDialect {

    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
        functionRegistry.registerPattern("fts", "?1 @@ ?2");
    }

}
