package me.z7087.blockminer.multiversion;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.entry.RegistryEntry;

public final class EntityAttributesImpl {
    private EntityAttributesImpl() {}
    public static final RegistryEntry<EntityAttribute> BLOCK_BREAK_SPEED = EntityAttributes.PLAYER_BLOCK_BREAK_SPEED;
    public static final RegistryEntry<EntityAttribute> SUBMERGED_MINING_SPEED = EntityAttributes.PLAYER_SUBMERGED_MINING_SPEED;
}
