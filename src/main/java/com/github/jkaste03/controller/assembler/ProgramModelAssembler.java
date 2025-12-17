package com.github.jkaste03.controller.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.github.jkaste03.controller.ProgramController;
import com.github.jkaste03.dto.response.ProgramResponse;
import com.github.jkaste03.model.Program.Status;

@Component
public class ProgramModelAssembler
        implements RepresentationModelAssembler<ProgramResponse, EntityModel<ProgramResponse>> {

    @Override
    @NonNull
    public EntityModel<ProgramResponse> toModel(@NonNull ProgramResponse program) {
        long programId = program.id();
        EntityModel<ProgramResponse> model = EntityModel.of(program,
                programSelfLink(programId),
                programActionLink(programId, "update"),
                programActionLink(programId, "delete"),
                programsCollectionLink());

        // HATEOAS-logikk:
        // Vi tilbyr kun handlinger som er gyldige for NÅVÆRENDE tilstand.

        // Kan kun starte hvis det faktisk er stoppet
        if (program.status() == Status.STOPPED) {
            model.add(linkTo(methodOn(ProgramController.class)
                    .startProgram(programId, null, null)).withRel("start"));
        }

        // Kan kun stoppe hvis det faktisk er startet
        else if (program.status() == Status.STARTED) {
            model.add(linkTo(methodOn(ProgramController.class)
                    .stopProgram(programId, null, null)).withRel("stop"));
        }

        // I tilstandene TRYING_TO_START og TRYING_TO_STOP legges INGEN action-linker
        // til.
        // Frontend vil dermed skjule/disable knappene.

        return model;
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
