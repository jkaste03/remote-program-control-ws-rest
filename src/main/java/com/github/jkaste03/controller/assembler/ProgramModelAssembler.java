package com.github.jkaste03.controller.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.github.jkaste03.controller.ProgramController;
import com.github.jkaste03.dto.response.ProgramResponse;

@Component
public class ProgramModelAssembler
        implements RepresentationModelAssembler<ProgramResponse, EntityModel<ProgramResponse>> {

    @Override
    @NonNull
    public EntityModel<ProgramResponse> toModel(@NonNull ProgramResponse program) {
        long programId = program.id();
        return EntityModel.of(program,
                programSelfLink(programId),
                programActionLink(programId, "update"),
                programActionLink(programId, "delete"),
                programsCollectionLink());
    }

    @Override
    @NonNull
    public CollectionModel<EntityModel<ProgramResponse>> toCollectionModel(
            @NonNull Iterable<? extends ProgramResponse> programs) {
        CollectionModel<EntityModel<ProgramResponse>> collection = RepresentationModelAssembler.super.toCollectionModel(
                programs);
        collection.add(programsCollectionLink());
        collection.add(linkTo(ProgramController.class).withRel("create"));
        return collection;
    }

    @NonNull
    private Link programsCollectionLink() {
        return linkTo(ProgramController.class)
                .withRel(IanaLinkRelations.COLLECTION.value());
    }

    @NonNull
    private Link programSelfLink(long programId) {
        return linkTo(ProgramController.class).slash(programId).withSelfRel();
    }

    @NonNull
    private Link programActionLink(long programId, @NonNull String rel) {
        return linkTo(ProgramController.class).slash(programId).withRel(rel);
    }

    // Program start/stop hateoas?
}
