/*
    Copyright 2009 Lunatech Research

    This file is part of jax-doclets.

    jax-doclets is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    jax-doclets is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with jax-doclets.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.lunatech.doclets.jax.jaxrs.writers;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.lunatech.doclets.jax.Utils;
import com.lunatech.doclets.jax.Utils.InvalidJaxTypeException;
import com.lunatech.doclets.jax.Utils.JaxType;
import com.lunatech.doclets.jax.jaxrs.JAXRSDoclet;
import com.lunatech.doclets.jax.jaxrs.model.JAXRSApplication;
import com.lunatech.doclets.jax.jaxrs.model.MethodOutput;
import com.lunatech.doclets.jax.jaxrs.model.MethodParameter;
import com.lunatech.doclets.jax.jaxrs.model.PojoTypes;
import com.lunatech.doclets.jax.jaxrs.model.ResourceMethod;
import com.lunatech.doclets.jax.jaxrs.tags.HTTPTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.RequestHeaderTaglet;
import com.lunatech.doclets.jax.jaxrs.tags.ResponseHeaderTaglet;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.tools.doclets.formats.html.TagletOutputImpl;

public class MethodWriter extends DocletWriter {

  private ResourceMethod method;

  public MethodWriter(ResourceMethod method, ResourceWriter resourceWriter, JAXRSDoclet doclet, JAXRSApplication application) {
    super(resourceWriter.getConfiguration(), resourceWriter.getWriter(), application, resourceWriter.getResource(), doclet);
    this.method = method;
  }

  public void print(PojoTypes types) {
    for (String httpMethod : method.getMethods()) {
      printMethod(httpMethod, types);
    }
  }

  private void printMethod(String httpMethod, PojoTypes types) {
    around("a name='" + httpMethod + "'", "");
    around("h3", httpMethod + " " + Utils.getAbsolutePath(this, resource));
    if (!Utils.isEmptyOrNull(method.getDoc())) {
      open("p");
      writer.printInlineComment(method.getJavaDoc());
      close("p");
    }
    printIncludes();
    open("dl");
    boolean doubleExample = getJAXRSConfiguration().enableHTTPExample
        && getJAXRSConfiguration().enableJavaScriptExample;
    if (doubleExample) {
      open("table class='examples'", "tr");
      open("td");
    }
    if (getJAXRSConfiguration().enableHTTPExample) {
      printHTTPExample(httpMethod);
    }
    if (doubleExample) {
      close("td");
      open("td");
    }
    if (getJAXRSConfiguration().enableJavaScriptExample) {
      printAPIExample();
    }
    if (doubleExample) {
      close("td");
      close("tr", "table");
    }
    printInput(types);
    printOutput(types);
    printParameters(method.getQueryParameters(), "Query");
    // done on resource
    // printParameters(method.getPathParameters(), "Path");
    printParameters(method.getMatrixParameters(), "Matrix");
    printParameters(method.getFormParameters(), "Form");
    printParameters(method.getCookieParameters(), "Cookie");
    printParameters(method.getHeaderParameters(), "Header");
    printMIMEs(method.getProduces(), "Produces");
    printMIMEs(method.getConsumes(), "Consumes");
    printHTTPCodes();
    printHTTPRequestHeaders();
    printHTTPResponseHeaders();

    printSince(method.getJavaDoc());
    printSeeAlso(method.getJavaDoc());
    // printSees();
    close("dl");
  }

  private void printIncludes() {
    MethodDoc javaDoc = method.getJavaDoc();
    Tag[] includes = Utils.getTags(javaDoc, "include");
    if (includes == null)
      return;
    File relativeTo = javaDoc.containingClass().position().file().getParentFile();
    for (Tag include : includes) {
      String fileName = include.text();
      File file = new File(relativeTo, fileName);
      if (!file.exists()) {
        doclet.printError(include.position(), "Missing included file: " + fileName);
        continue;
      }
      String text = Utils.readResource(file);
      print(text);
    }
  }

  /*
   * Path won't be to jaxb class private void printSees() { MethodDoc javaDoc =
   * method.getJavaDoc(); TagletOutputImpl output = new TagletOutputImpl("");
   * Set<String> tagletsSet = new HashSet<String>(); tagletsSet.add("see");
   * Utils.genTagOuput(configuration.tagletManager, javaDoc,
   * configuration.tagletManager.getCustomTags(javaDoc), writer
   * .getTagletWriterInstance(false), output, tagletsSet);
   * writer.print(output.toString()); }
   */
  private void printTaglets(String tagletName) {
    MethodDoc javaDoc = method.getJavaDoc();
    TagletOutputImpl output = new TagletOutputImpl("");
    Set<String> tagletsSet = new HashSet<String>();
    tagletsSet.add(tagletName);
    Utils.genTagOuput(configuration.parentConfiguration.tagletManager, javaDoc,
                      configuration.parentConfiguration.tagletManager.getCustomTags(javaDoc), writer.getTagletWriterInstance(false),
                      output, tagletsSet);
    writer.print(output.toString());
  }

  private void printHTTPRequestHeaders() {
    printTaglets(RequestHeaderTaglet.NAME);
  }

  private void printHTTPResponseHeaders() {
    printTaglets(ResponseHeaderTaglet.NAME);
  }

  private void printHTTPCodes() {
    printTaglets(HTTPTaglet.NAME);
  }

  private void printOutput(PojoTypes types) {
    open("dt");
    around("b", "Output:");
    close("dt");

    MethodOutput output = method.getOutput();

    if (output.isOutputWrapped()) {
      for (int i = 0; i < output.getOutputWrappedCount(); i++) {
        open("dd");
        String typeName = output.getWrappedOutputType(i);
        JaxType returnType = null;
        try {
          returnType = Utils.parseType(typeName, method.getJavaDoc().containingClass(), doclet);
        } catch (InvalidJaxTypeException e) {
          doclet.warn("Invalid @returnWrapped type: " + typeName);
          e.printStackTrace();
        }
        if (returnType != null) {
          printOutputType(returnType, types);
        } else {
          around("tt", escape(typeName));
        }
        if (output.getOutputDoc(i) != null) {
          print(" - ");
          print(output.getOutputDoc(i));
        }
        close("dd");
      }
    } else {
      open("dd");
      Type returnType = output.getOutputType();
      printOutputType(returnType, types);
      if (output.getOutputDoc() != null) {
        print(" - ");
        print(output.getOutputDoc());
      }
      close("dd");
    }
  }

  private void printOutputType(JaxType type, PojoTypes types) {
    if (type.getType() == null) {
      doclet.warn("Type information not found: " + type.getTypeName());
      print(type.getTypeName());
      return;
    }
    open("tt");
    printOutputGenericType(type.getType(), types);
    if (type.hasParameters()) {
      boolean first = true;
      print("&lt;");
      for (JaxType genericType : type.getParameters()) {
        if (first) {
          first = false;
        } else {
          print(",");
        }
        printOutputGenericType(genericType.getType(), types);
      }
      print("&gt;");
    }
    close("tt");
  }

  private void printOutputType(Type type, PojoTypes types) {
    open("tt");
    printOutputGenericType(type, types);
    close("tt");
  }

  private void printOutputGenericType(Type type, PojoTypes types) {
    String link = null;
    if (!type.isPrimitive()) {
      link = Utils.getExternalLink(configuration.parentConfiguration, type, writer);

      if ((link == null) && getJAXRSConfiguration().enablePojoJsonDataObjects) {
        if (types.resolveUsedType(type)) {
          link = Utils.urlToRoot(getResource()) + DataObjectIndexWriter.getLink(type.asClassDoc());
        }
      }
    }

    if (link == null) {
      around("span title='" + type.qualifiedTypeName() + "'", type.simpleTypeName());
    } else {
      around("a title='" + type.qualifiedTypeName() + "' + href='" + link + "'", type.simpleTypeName());
    }
    ParameterizedType pType = type.asParameterizedType();
    if (pType != null) {
      boolean first = true;
      print("&lt;");
      for (Type genericType : pType.typeArguments()) {
        if (first) {
          first = false;
        } else {
          print(",");
        }
        printOutputGenericType(genericType, types);
      }
      print("&gt;");
    }
    print(type.dimension());
  }

  private void printInput(PojoTypes types) {
    MethodParameter inputParameter = method.getInputParameter();
    if (inputParameter == null)
      return;
    open("dt");
    around("b", "Input:");
    close("dt");
    if (inputParameter.isWrapped()) {
      open("dd");
      String typeName = inputParameter.getWrappedType();
      JaxType returnType = null;
      try {
        returnType = Utils.parseType(typeName, method.getJavaDoc().containingClass(), doclet);
      } catch (InvalidJaxTypeException e) {
        doclet.warn("Invalid @returnWrapped type: " + typeName);
        e.printStackTrace();
      }
      if (returnType != null)
        printOutputType(returnType, types);
      else
        around("tt", escape(typeName));
    } else {
      open("dd");
      Type returnType = inputParameter.getType();
      printOutputType(returnType, types);
    }
    String doc = inputParameter.getDoc();
    if (!Utils.isEmptyOrNull(doc)) {
      print(" - ");
      print(doc);
    }
    close("dd");
  }

  private void printParameters(List<MethodParameter> parameters, String header) {
    if (parameters.isEmpty())
      return;
    open("dt");
    around("b", header + " parameters:");
    close("dt");
    for (MethodParameter param : parameters) {
      open("dd");
      around("b", param.getName());
      String doc = param.getDoc();
      if (!Utils.isEmptyOrNull(doc))
        print(" - " + param.getDoc());
      close("dd");
    }
  }

  private void printMIMEs(List<String> mimes, String header) {
    if (!mimes.isEmpty()) {
      open("dt");
      around("b", header + ":");
      close("dt");
      for (String mime : mimes) {
        open("dd");
        print(mime);
        close("dd");
      }
    }
  }

  private void printAPIExample() {
    around("dt", "API Example:");
    open("dd");
    /*
     * We are using tt instead of pre to avoid whitespace issues in the doc's
     * first sentence tags that would show up in a pre and would not in a tt.
     * This is annoying.
     */
    open("p");
    open("tt");
    print(method.getAPIFunctionName());
    print("({");
    boolean hasOne = printAPIParameters(method.getMatrixParameters(), false);
    hasOne |= printAPIParameters(method.getQueryParameters(), hasOne);
    hasOne |= printAPIParameters(method.getPathParameters(), hasOne);
    hasOne |= printAPIParameters(method.getHeaderParameters(), hasOne);
    hasOne |= printAPIParameters(method.getCookieParameters(), hasOne);
    hasOne |= printAPIParameters(method.getFormParameters(), hasOne);
    MethodParameter input = method.getInputParameter();
    if (input != null) {
      printAPIParameter("$entity", input, hasOne);
    }
    print("});");
    close("tt");
    close("p");
    close("dd");
  }

  private boolean printAPIParameters(List<MethodParameter> parameters, boolean hasOne) {
    for (MethodParameter parameter : parameters) {
      printAPIParameter(parameter.getName(), parameter, hasOne);
      hasOne = true;
    }
    return hasOne;
  }

  private void printAPIParameter(String name, MethodParameter param, boolean hasOne) {
    if (hasOne) {
      print(",");
      tag("br");
      print("&nbsp;&nbsp;");
    }
    hasOne = true;
    print("'" + name + "': ");
    Tag[] tags = param.getFirstSentenceTags();
    if (tags != null && tags.length > 0) {
      open("span class='comment'");
      print("/* ");
      writer.printSummaryComment(param.getParameterDoc(), tags);
      print(" */");
      close("span");
    } else
      print("…");
  }

  private void printHTTPExample(String httpMethod) {
    around("dt", "HTTP Example:");
    open("dd");
    open("pre");
    String absPath = Utils.getAbsolutePath(this, resource);

    print(httpMethod + " " + absPath);
    List<MethodParameter> matrixParameters = method.getMatrixParameters();
    if (!matrixParameters.isEmpty()) {
      for (MethodParameter parameter : matrixParameters) {
        print(";");
        print(parameter.getName());
        print("=…");
      }
    }
    List<MethodParameter> queryParameters = method.getQueryParameters();
    if (!queryParameters.isEmpty()) {
      boolean first = true;
      for (MethodParameter parameter : queryParameters) {
        if (first) {
          print("?");
        } else {
          print("&amp;");
        }
        print(parameter.getName());
        print("=…");
        first = false;
      }
    }

    List<MethodParameter> headerParameters = method.getHeaderParameters();
    if (!headerParameters.isEmpty()) {
      print("\n");
      Iterator<MethodParameter> params = headerParameters.iterator();
      while(params.hasNext()) {
        MethodParameter parameter = params.next();
        print(parameter.getName());
        print(": …");
        if (params.hasNext()) {
          print("\n");
        }
      }
    }
    List<MethodParameter> cookieParameters = method.getCookieParameters();
    if (!cookieParameters.isEmpty()) {
      print("\n");
      Iterator<MethodParameter> params = headerParameters.iterator();
      while (params.hasNext()) {
        MethodParameter parameter = params.next();
        print("Cookie: ");
        print(parameter.getName());
        print(": …");
        if (params.hasNext()) {
          print("\n");
        }
      }
    }

    if (!method.getFormParameters().isEmpty()) {
      boolean first = true;
      for (MethodParameter parameter : method.getFormParameters()) {
        if(first) {
          print("\n");
        } else {
          print("&amp;");
        }
        print(parameter.getName());
        print("=…");
        first = false;
      }
    }
    close("pre");
    close("dd");
  }
}
