package io.github.freshsupasulley.censorcraft.common.jscribe;

import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Incredibly disgusting SLF4J wrapper that caps every log level at debug. I don't know how to setup logback for another logger in forge just for whisper-jni.
 */
public class DebugLogger implements Logger {
	
	private final Logger delegate;
	
	public DebugLogger(Logger delegate)
	{
		this.delegate = delegate;
	}
	
	@Override
	public String getName()
	{
		return delegate.getName();
	}
	
	@Override
	public void trace(String msg)
	{
		delegate.trace(msg);
	}
	
	@Override
	public void trace(String format, Object arg)
	{
		delegate.trace(format, arg);
	}
	
	@Override
	public void trace(String format, Object arg1, Object arg2)
	{
		delegate.trace(format, arg1, arg2);
	}
	
	@Override
	public void trace(String format, Object... arguments)
	{
		delegate.trace(format, arguments);
	}
	
	@Override
	public void trace(String msg, Throwable t)
	{
		delegate.trace(msg, t);
	}
	
	@Override
	public void debug(String msg)
	{
		delegate.debug(msg);
	}
	
	@Override
	public void debug(String format, Object arg)
	{
		delegate.debug(format, arg);
	}
	
	@Override
	public void debug(String format, Object arg1, Object arg2)
	{
		delegate.debug(format, arg1, arg2);
	}
	
	@Override
	public void debug(String format, Object... arguments)
	{
		delegate.debug(format, arguments);
	}
	
	@Override
	public void debug(String msg, Throwable t)
	{
		delegate.debug(msg, t);
	}
	
	@Override
	public void info(String msg)
	{
		delegate.debug(msg);
	}
	
	@Override
	public void info(String format, Object arg)
	{
		delegate.debug(format, arg);
	}
	
	@Override
	public void info(String format, Object arg1, Object arg2)
	{
		delegate.debug(format, arg1, arg2);
	}
	
	@Override
	public void info(String format, Object... arguments)
	{
		delegate.debug(format, arguments);
	}
	
	@Override
	public void info(String msg, Throwable t)
	{
		delegate.debug(msg, t);
	}
	
	@Override
	public void warn(String msg)
	{
		delegate.debug(msg);
	}
	
	@Override
	public void warn(String format, Object arg)
	{
		delegate.debug(format, arg);
	}
	
	@Override
	public void warn(String format, Object arg1, Object arg2)
	{
		delegate.debug(format, arg1, arg2);
	}
	
	@Override
	public void warn(String format, Object... arguments)
	{
		delegate.debug(format, arguments);
	}
	
	@Override
	public void warn(String msg, Throwable t)
	{
		delegate.debug(msg, t);
	}
	
	@Override
	public void error(String msg)
	{
		delegate.debug(msg);
	}
	
	@Override
	public void error(String format, Object arg)
	{
		delegate.debug(format, arg);
	}
	
	@Override
	public void error(String format, Object arg1, Object arg2)
	{
		delegate.debug(format, arg1, arg2);
	}
	
	@Override
	public void error(String format, Object... arguments)
	{
		delegate.debug(format, arguments);
	}
	
	@Override
	public void error(String msg, Throwable t)
	{
		delegate.debug(msg, t);
	}
	
	@Override
	public boolean isTraceEnabled()
	{
		return delegate.isTraceEnabled();
	}
	
	@Override
	public boolean isDebugEnabled()
	{
		return delegate.isDebugEnabled();
	}
	
	@Override
	public boolean isInfoEnabled()
	{
		return delegate.isInfoEnabled();
	}
	
	@Override
	public boolean isWarnEnabled()
	{
		return delegate.isWarnEnabled();
	}
	
	@Override
	public boolean isErrorEnabled()
	{
		return delegate.isErrorEnabled();
	}
	
	@Override
	public boolean isTraceEnabled(Marker marker)
	{
		return delegate.isTraceEnabled(marker);
	}
	
	@Override
	public boolean isDebugEnabled(Marker marker)
	{
		return delegate.isDebugEnabled(marker);
	}
	
	@Override
	public boolean isInfoEnabled(Marker marker)
	{
		return delegate.isInfoEnabled(marker);
	}
	
	@Override
	public boolean isWarnEnabled(Marker marker)
	{
		return delegate.isWarnEnabled(marker);
	}
	
	@Override
	public boolean isErrorEnabled(Marker marker)
	{
		return delegate.isErrorEnabled(marker);
	}
	
	@Override
	public void trace(Marker marker, String msg)
	{
		delegate.trace(marker, msg);
	}
	
	@Override
	public void trace(Marker marker, String format, Object arg)
	{
		delegate.trace(marker, format, arg);
	}
	
	@Override
	public void trace(Marker marker, String format, Object arg1, Object arg2)
	{
		delegate.trace(marker, format, arg1, arg2);
	}
	
	@Override
	public void trace(Marker marker, String format, Object... arguments)
	{
		delegate.trace(marker, format, arguments);
	}
	
	@Override
	public void trace(Marker marker, String msg, Throwable t)
	{
		delegate.trace(marker, msg, t);
	}
	
	@Override
	public void debug(Marker marker, String msg)
	{
		delegate.debug(marker, msg);
	}
	
	@Override
	public void debug(Marker marker, String format, Object arg)
	{
		delegate.debug(marker, format, arg);
	}
	
	@Override
	public void debug(Marker marker, String format, Object arg1, Object arg2)
	{
		delegate.debug(marker, format, arg1, arg2);
	}
	
	@Override
	public void debug(Marker marker, String format, Object... arguments)
	{
		delegate.debug(marker, format, arguments);
	}
	
	@Override
	public void debug(Marker marker, String msg, Throwable t)
	{
		delegate.debug(marker, msg, t);
	}
	
	@Override
	public void info(Marker marker, String msg)
	{
		delegate.debug(marker, msg);
	}
	
	@Override
	public void info(Marker marker, String format, Object arg)
	{
		delegate.debug(marker, format, arg);
	}
	
	@Override
	public void info(Marker marker, String format, Object arg1, Object arg2)
	{
		delegate.debug(marker, format, arg1, arg2);
	}
	
	@Override
	public void info(Marker marker, String format, Object... arguments)
	{
		delegate.debug(marker, format, arguments);
	}
	
	@Override
	public void info(Marker marker, String msg, Throwable t)
	{
		delegate.debug(marker, msg, t);
	}
	
	@Override
	public void warn(Marker marker, String msg)
	{
		delegate.debug(marker, msg);
	}
	
	@Override
	public void warn(Marker marker, String format, Object arg)
	{
		delegate.debug(marker, format, arg);
	}
	
	@Override
	public void warn(Marker marker, String format, Object arg1, Object arg2)
	{
		delegate.debug(marker, format, arg1, arg2);
	}
	
	@Override
	public void warn(Marker marker, String format, Object... arguments)
	{
		delegate.debug(marker, format, arguments);
	}
	
	@Override
	public void warn(Marker marker, String msg, Throwable t)
	{
		delegate.debug(marker, msg, t);
	}
	
	@Override
	public void error(Marker marker, String msg)
	{
		delegate.debug(marker, msg);
	}
	
	@Override
	public void error(Marker marker, String format, Object arg)
	{
		delegate.debug(marker, format, arg);
	}
	
	@Override
	public void error(Marker marker, String format, Object arg1, Object arg2)
	{
		delegate.debug(marker, format, arg1, arg2);
	}
	
	@Override
	public void error(Marker marker, String format, Object... arguments)
	{
		delegate.debug(marker, format, arguments);
	}
	
	@Override
	public void error(Marker marker, String msg, Throwable t)
	{
		delegate.debug(marker, msg, t);
	}
}
