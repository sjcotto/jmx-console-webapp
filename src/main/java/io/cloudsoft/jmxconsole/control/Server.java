package io.cloudsoft.jmxconsole.control;

import io.cloudsoft.jmxconsole.compatibility.Classes;
import io.cloudsoft.jmxconsole.compatibility.Logger;
import io.cloudsoft.jmxconsole.compatibility.MBeanServerLocator;
import io.cloudsoft.jmxconsole.model.DomainData;
import io.cloudsoft.jmxconsole.model.MBeanData;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;


/** Utility methods related to the MBeanServer interface
 *
 * @author Scott.Stark@jboss.org
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 * @version $Revision: 81038 $
 */
public class Server
{
   static Logger log = Logger.getLogger(Server.class);

   public static MBeanServer getMBeanServer()
   {
      return MBeanServerLocator.locateMBeanServer();
   }

   public static Iterator getDomainData(String filter) throws JMException
   {
      MBeanServer server = getMBeanServer();
      TreeMap domainData = new TreeMap();
      if( server != null )
      {
         ObjectName filterName = null;
         if( filter != null )
            filterName = new ObjectName(filter);
         Set objectNames = server.queryNames(filterName, null);
         Iterator objectNamesIter = objectNames.iterator();
         while( objectNamesIter.hasNext() )
         {
            ObjectName name = (ObjectName) objectNamesIter.next();
            MBeanInfo info = server.getMBeanInfo(name);
            String domainName = name.getDomain();
            MBeanData mbeanData = new MBeanData(name, info);
            DomainData data = (DomainData) domainData.get(domainName);
            if( data == null )
            {
               data = new DomainData(domainName);
               domainData.put(domainName, data);
            }
            data.addData(mbeanData);
         }
      }
      Iterator domainDataIter = domainData.values().iterator();
      return domainDataIter;
   }

   public static MBeanData getMBeanData(String name) throws JMException
   {
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      MBeanInfo info = server.getMBeanInfo(objName);
      MBeanData mbeanData = new MBeanData(objName, info);
      return mbeanData;
   }

   public static Object getMBeanAttributeObject(String name, String attrName)
      throws JMException
   {
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      Object value = server.getAttribute(objName, attrName);
      return value;
   }

   public static String getMBeanAttribute(String name, String attrName) throws JMException
   {
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      String value = null;
      try
      {
         Object attr = server.getAttribute(objName, attrName);
         if( attr != null )
            value = attr.toString();
      }
      catch(JMException e)
      {
         value = e.getMessage();
      }
      return value;
   }

   public static AttrResultInfo getMBeanAttributeResultInfo(String name, MBeanAttributeInfo attrInfo)
      throws JMException
   {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      String attrName = attrInfo.getName();
      String attrType = attrInfo.getType();
      Object value = null;
      Throwable throwable = null;
      if( attrInfo.isReadable() == true )
      {
         try
         {
            value = server.getAttribute(objName, attrName);
         }
         catch (Throwable t)
         {
            throwable = t;
         }
      }
      Class typeClass = null;
      try
      {
         typeClass = Classes.getPrimitiveTypeForName(attrType);
         if( typeClass == null )
            typeClass = loader.loadClass(attrType);
      }
      catch(ClassNotFoundException ignore)
      {
      }
      PropertyEditor editor = null;
      if( typeClass != null )
         editor = PropertyEditorManager.findEditor(typeClass);

      return new AttrResultInfo(attrName, editor, value, throwable);
   }

   public static AttributeList setAttributes(String name, HashMap attributes) throws JMException
   {
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      MBeanInfo info = server.getMBeanInfo(objName);
      MBeanAttributeInfo[] attributesInfo = info.getAttributes();
      AttributeList newAttributes = new AttributeList();
      for(int a = 0; a < attributesInfo.length; a ++)
      {
         MBeanAttributeInfo attrInfo = attributesInfo[a];
         String attrName = attrInfo.getName();
         if( attributes.containsKey(attrName) == false )
            continue;
         String value = (String) attributes.get(attrName);
         if (value.equals("null") && server.getAttribute(objName, attrName) == null) {
            log.trace("ignoring 'null' for " + attrName);
            continue;
         }
         String attrType = attrInfo.getType();
         Attribute attr = null;
         // TODO so far we don't support conversion; Brooklyn.TypeCoercions would be userul here
//         try
//         {
            throw new UnsupportedOperationException("conversion of '"+value+"' to "+attrType+" not supported");
//            Object realValue = PropertyEditors.convertValue(value, attrType);
//            attr = new Attribute(attrName, realValue);
//         }
//         catch(ClassNotFoundException e)
//         {
//            String s = (attr != null) ? attr.getName() : attrType;
//            log.trace("Failed to load class for attribute: " + s, e);
//            throw new ReflectionException(e, "Failed to load class for attribute: " + s);
//         }
//         catch(IntrospectionException e)
//         {
//            log.trace("Skipped setting attribute: " + attrName + 
//                    ", cannot find PropertyEditor for type: " + attrType);
//            continue;
//         }
//
//         server.setAttribute(objName, attr);
//         newAttributes.add(attr);
      }
      return newAttributes;
   }

   public static OpResultInfo invokeOp(String name, int index, String[] args) throws JMException
   {
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      MBeanInfo info = server.getMBeanInfo(objName);
      MBeanOperationInfo[] opInfo = info.getOperations();
      MBeanOperationInfo op = opInfo[index];
      MBeanParameterInfo[] paramInfo = op.getSignature();
      String[] argTypes = new String[paramInfo.length];
      for(int p = 0; p < paramInfo.length; p ++)
         argTypes[p] = paramInfo[p].getType();
      return invokeOpByName(name, op.getName(), argTypes, args);
   }

   public static OpResultInfo invokeOpByName(String name, String opName,
      String[] argTypes, String[] args)
      throws JMException
   {
      MBeanServer server = getMBeanServer();
      ObjectName objName = new ObjectName(name);
      int length = argTypes != null ? argTypes.length : 0;
      Object[] typedArgs = new Object[length];
      for(int p = 0; p < typedArgs.length; p ++)
      {
         String arg = args[p];
         // TODO so far we don't support conversion; Brooklyn.TypeCoercions would be userul here
//       try
//       {
          throw new UnsupportedOperationException("conversion of '"+arg+"' to "+argTypes[p]+" not supported");
//            Object argValue = PropertyEditors.convertValue(arg, argTypes[p]);
//            typedArgs[p] = argValue;
//         }
//         catch(ClassNotFoundException e)
//         {
//            log.trace("Failed to load class for arg"+p, e);
//            throw new ReflectionException(e, "Failed to load class for arg"+p);
//         }
//         catch(java.beans.IntrospectionException e)
//         {
//            // If the type is not java.lang.Object throw an exception
//            if( argTypes[p].equals("java.lang.Object") == false )
//               throw new javax.management.IntrospectionException(
//                  "Failed to find PropertyEditor for type: "+argTypes[p]);
//            // Just use the String arg
//            typedArgs[p] = arg;
//            continue;
//         }
      }
      Object opReturn = server.invoke(objName, opName, typedArgs, argTypes);
      return new OpResultInfo(opName, argTypes, args, opReturn);
   }
}
