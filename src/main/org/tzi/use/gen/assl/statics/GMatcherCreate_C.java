/*
 * This is source code of the Snapshot Generator, an extension for USE
 * to generate (valid) system states of UML models.
 * Copyright (C) 2001 Joern Bohling, University of Bremen
 *
 * About USE:
 *   USE - UML based specification environment
 *   Copyright (C) 1999,2000,2001 Mark Richters, University of Bremen
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.tzi.use.gen.assl.statics;

import org.tzi.use.uml.mm.MModel;
import java.util.List;

/**
 * Matcher for a Create(Class) instruction
 * 
 * @see org.tzi.use.gen.assl.statics
 * @author  Joern Bohling
 */
class GMatcherCreate_C implements IGInstructionMatcher {
    public String name() {
        return "Create";
    }

    public GInstruction createIfMatches( List<Object> param, MModel model ) {
        if (!matches(param, model))
            return null;
        return new GInstrCreate_C(  model.getClass((String) param.get(0))  );
    }

    private boolean matches( List<Object> param, MModel model ) {
        return
            param.size() == 1 &&
            param.get(0) instanceof String &&
            model.getClass((String) param.get(0)) != null &&
            ! model.getClass((String) param.get(0)).isAbstract();
    }
}
