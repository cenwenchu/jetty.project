package org.eclipse.jetty.websocket.driver;


/**
 * A representation of the methods available to call for a particular class.
 * <p>
 * This class used to cache the method lookups via the {@link EventMethodsCache}
 */
public class EventMethods
{
    private Class<?> pojoClass;
    private boolean isAnnotated = false;
    public EventMethod onConnect = null;
    public EventMethod onClose = null;
    public EventMethod onBinary = null;
    public EventMethod onBinaryStream = null;
    public EventMethod onText = null;
    public EventMethod onTextStream = null;
    public EventMethod onException = null;
    public EventMethod onFrame = null;

    public EventMethods(Class<?> pojoClass, boolean annotated)
    {
        this.pojoClass = pojoClass;
        this.isAnnotated = annotated;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        EventMethods other = (EventMethods)obj;
        if (pojoClass == null)
        {
            if (other.pojoClass != null)
            {
                return false;
            }
        }
        else if (!pojoClass.getName().equals(other.pojoClass.getName()))
        {
            return false;
        }
        return true;
    }

    public Class<?> getPojoClass()
    {
        return pojoClass;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((pojoClass == null)?0:pojoClass.getName().hashCode());
        return result;
    }

    public boolean isAnnotated()
    {
        return isAnnotated;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("EventMethods [pojoClass=");
        builder.append(pojoClass);
        builder.append(", isAnnotated=");
        builder.append(isAnnotated);
        builder.append(", onConnect=");
        builder.append(onConnect);
        builder.append(", onClose=");
        builder.append(onClose);
        builder.append(", onBinary=");
        builder.append(onBinary);
        builder.append(", onText=");
        builder.append(onText);
        builder.append(", onException=");
        builder.append(onException);
        builder.append(", onFrame=");
        builder.append(onFrame);
        builder.append("]");
        return builder.toString();
    }

}