const pipelineComponentCopyToRepository = {
    value: "CopyToRepository",
    visible: true,
    customProperties: [
        "destinationContainerRepoDomain",
        "destinationContainerRepoId",
        "tag",
    ],
};

const pipelineComponentManualPromotionGate = {
    value: "ManualPromotionGate",
    visible: false,
    customProperties: [],
};

export const types = {
    copyToRepository: pipelineComponentCopyToRepository,
    manualPromotionGate: pipelineComponentManualPromotionGate,
};

export function guessPipelineComponentType(component) {
    let result = pipelineComponentManualPromotionGate;
    pipelineComponentCopyToRepository.customProperties.forEach(propertyName => {
        if (component.hasOwnProperty(propertyName)) {
            result = pipelineComponentCopyToRepository;
        }
    });

    return result;
};
