package org.snomed.snowstorm.snomedConverter;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.snomed.snowstorm.snomedConverter.converterPipeline.InputTokenizer;
import org.snomed.snowstorm.snomedConverter.converterPipeline.TokenMatchingMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/converter", produces = "application/json")
public class ConverterController {

    @Autowired
    TokenMatchingMatrix tokenMatchingMatrix;

    @GetMapping(value = "/{sequence}")
    public String foo(@PathVariable String sequence){
        List<String> tokenList = InputTokenizer.tokenize(sequence);
        tokenMatchingMatrix.generateLexicon(tokenList);
        return "hallo";
    }
}
