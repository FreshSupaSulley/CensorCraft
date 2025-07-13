package io.github.freshsupasulley.plugins.impl;

import io.github.freshsupasulley.censorcraft.api.events.Event;

public class EventImpl implements Event {
	
	private boolean cancelled;
	
	@Override
	public boolean isCancellable()
	{
		return false;
	}
	
	@Override
	public boolean cancel()
	{
		if(!isCancellable())
		{
			return false;
		}
		
		cancelled = true;
		return true;
	}
	
	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}
}
