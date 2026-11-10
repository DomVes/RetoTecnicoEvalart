package com.evalart.automation.runners;

import cucumber.api.CucumberOptions;
import cucumber.api.SnippetType;
import net.serenitybdd.cucumber.CucumberWithSerenity;
import org.junit.runner.RunWith;

@RunWith(CucumberWithSerenity.class)
@CucumberOptions(
        features = "src/test/resources/features/completar_formulario.feature",
        glue = "com/evalart/automation/stepdefinitions",
        snippets = SnippetType.CAMELCASE
)
public class CompletarFormularioRunner {
}
