package org.gensokyo.plugin.easydoc.plan;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class PlanValidationResult {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();

    public static PlanValidationResult ok() {
        return new PlanValidationResult();
    }

    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }
}
