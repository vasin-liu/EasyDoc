package org.gensokyo.plugin.easydoc.plan;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DbRef {
    private String dataSource;
    private String namespace;
    private String objectName;
    private String objectKind;
}
