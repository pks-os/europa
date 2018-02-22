package com.distelli.europa.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.OptionalInt;
import java.util.stream.IntStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pipeline
{
    private String domain;
    private String id;
    // Trigger for pipeline execution:
    private String containerRepoId;
    private String name;
    @Singular
    private List<PipelineComponent> components;

    public OptionalInt getComponentIndex(String componentId) {
        return IntStream.range(0, components.size())
            .filter((i) -> componentId.equalsIgnoreCase(components.get(i).getId()))
            .findFirst();
    }
}
