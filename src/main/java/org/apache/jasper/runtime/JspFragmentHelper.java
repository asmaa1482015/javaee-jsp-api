/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.apache.jasper.runtime;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.JspFragment;

/**
 * Helper class from which all Jsp Fragment helper classes extend.
 * This class allows for the emulation of numerous fragments within
 * a single class, which in turn reduces the load on the class loader
 * since there are potentially many JspFragments in a single page.
 * <p>
 * The class also provides various utility methods for JspFragment
 * implementations.
 *
 * @author Mark Roth
 */
public abstract class JspFragmentHelper 
    extends JspFragment 
{
    
    protected int discriminator;
    protected JspContext jspContext;
    protected PageContext _jspx_page_context;
    protected JspTag parentTag;

    public JspFragmentHelper( int discriminator, JspContext jspContext, 
        JspTag parentTag ) 
    {
        this.discriminator = discriminator;
        this.jspContext = jspContext;
        this._jspx_page_context = null;
        if( jspContext instanceof PageContext ) {
            _jspx_page_context = (PageContext)jspContext;
        }
        this.parentTag = parentTag;
    }
    
    public JspContext getJspContext() {
        return this.jspContext;
    }
    
    public JspTag getParentTag() {
        return this.parentTag;
    }
    
}