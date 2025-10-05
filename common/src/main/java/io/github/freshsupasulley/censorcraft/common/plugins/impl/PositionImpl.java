package io.github.freshsupasulley.censorcraft.common.plugins.impl;

import de.maxhenkel.voicechat.api.Position;
import net.minecraft.world.phys.Vec3;

public class PositionImpl implements Position {
	
	private Vec3 pos;
	
	public PositionImpl(Vec3 pos)
	{
		this.pos = pos;
	}
	
	@Override
	public double getX()
	{
		return pos.x;
	}
	
	@Override
	public double getY()
	{
		return pos.y;
	}
	
	@Override
	public double getZ()
	{
		return pos.z;
	}
}
