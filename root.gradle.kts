plugins {
    // This marks the current project as the root of a multi-version project.
    // Any project using `gg.essential.multi-version` must have a parent with this root plugin applied.
    // Advanced users may use multiple (potentially independent) multi-version trees in different sub-projects.
    // This is currently equivalent to applying `com.replaymod.preprocess-root`.
    id("gg.essential.multi-version.root")
}

preprocess {
    // Here you first need to create a node per version you support and assign it an integer Minecraft version.
    // The mappings value is currently meaningless.
    val fabric12101 = createNode("1.21.1-fabric", 12101, "yarn")
    val fabric12103 = createNode("1.21.3-fabric", 12103, "yarn")
    val fabric12104 = createNode("1.21.4-fabric", 12104, "yarn")

    // And then you need to tell the preprocessor which versions it should directly convert between.
    // This should form a directed graph with no cycles (i.e. a tree), which the preprocessor will then traverse to
    // produce source code for all versions from the main version.
    // Do note that the preprocessor can only convert between two projects when they are either on the same Minecraft
    // version (but use different mappings, e.g. 1.16.2 forge to fabric), or when they are using the same intermediary
    // mappings (but on different Minecraft versions, e.g. 1.12.2 forge to 1.8.9 forge, or 1.16.2 fabric to 1.18 fabric)
    // but not both at the same time, i.e. you cannot go straight from 1.12.2 forge to 1.16.2 fabric, you need to go via
    // an intermediary 1.16.2 forge project which has something in common with both.
    fabric12104.link(fabric12103)
    fabric12103.link(fabric12101)
    // For any link, you can optionally specify a file containing extra mappings which the preprocessor cannot infer by
    // itself, e.g. forge intermediary names do not contain class names, so you may need to supply mappings for those
    // manually.
    //forge11202.link(forge10809, file("versions/1.12.2-1.8.9.txt"))
}