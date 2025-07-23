package binhnvh.vaap.promptbuilder.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Entity
@Data
@Table(name = "prompt_variables")
public class PromptVariable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @NotEmpty
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$")
    @Column(name = "variable_name", nullable = false)
    private String variableName;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "description")
    private String description;

    @Column(name = "is_required")
    private boolean required = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type")
    private VariableType dataType = VariableType.STRING;

    public enum VariableType {
        STRING, NUMBER, BOOLEAN, JSON
    }
}