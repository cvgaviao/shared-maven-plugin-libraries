/**
 * ======================================================================
 * Copyright © 2015-2019, OSGi Alliance, Cristiano V. Gavião.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * =======================================================================
 */
package org.osgi.service.indexer.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.osgi.service.indexer.impl.Utils.findCaps;
import static org.osgi.service.indexer.impl.Utils.findReqs;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class BundleAnalyzerUnitTest {

    @Test
    public void testContentAndIdentity() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(new JarResource(new File(getClass()
                .getResource("/testdata/01-bsn+version.jar").getPath())), caps,
                reqs);

        assertEquals(4, caps.size());

        Capability idcap = caps.get(0);
        assertEquals("osgi.identity", idcap.getNamespace());
        assertEquals("org.example.a",
                idcap.getAttributes().get("osgi.identity"));
        assertEquals("osgi.bundle", idcap.getAttributes().get("type"));
        assertEquals(new Version("0.0.0"),
                idcap.getAttributes().get("version"));

        Capability content = caps.get(1);
        assertEquals("osgi.content", content.getNamespace());
        assertEquals(
                "64f661eea43334dc5d38d7f16dbcacd02c799e68332b40e72da8021828e3329c",
                content.getAttributes().get("osgi.content"));
        File url = new File((String) content.getAttributes().get("url"));
        assertEquals(true,
                url.toPath().endsWith("testdata/01-bsn+version.jar"));
        assertEquals("application/vnd.osgi.bundle",
                content.getAttributes().get("mime"));
        assertEquals(1104L, content.getAttributes().get("size"));
    }

    @Test
    public void testPackageExports() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(
                new JarResource(new File(getClass()
                        .getResource("/testdata/03-export.jar").getPath())),
                caps, reqs);

        Capability export = findCaps("osgi.wiring.package", caps).get(0);
        assertEquals("org.example.a",
                export.getAttributes().get("osgi.wiring.package"));
        assertEquals(new Version(1, 0, 0),
                export.getAttributes().get("version"));
    }

    @Test
    public void testPackageExportUses() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(new JarResource(new File(getClass()
                .getResource("/testdata/04-export+uses.jar").getPath())), caps,
                reqs);

        List<Capability> exports = findCaps("osgi.wiring.package", caps);
        assertEquals(2, exports.size());

        assertEquals("org.example.b",
                exports.get(0).getAttributes().get("osgi.wiring.package"));
        assertEquals("org.example.a",
                exports.get(0).getDirectives().get("uses"));
        assertEquals("org.example.a",
                exports.get(1).getAttributes().get("osgi.wiring.package"));
    }

    // bundle-symbolic-name and bundle-version must be on package capabilities,
    // for the idiots
    // who add this to their imports...
    @Test
    public void testPackageExportBundleSymbolicNameAndVersion()
            throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(new JarResource(new File(getClass()
                .getResource("/testdata/04-export+uses.jar").getPath())), caps,
                reqs);

        List<Capability> exports = findCaps("osgi.wiring.package", caps);
        assertEquals(2, exports.size());

        assertEquals("org.example.b",
                exports.get(0).getAttributes().get("osgi.wiring.package"));
        assertEquals("org.example.d",
                exports.get(0).getAttributes().get("bundle-symbolic-name"));
        assertEquals(new Version(0, 0, 0),
                exports.get(0).getAttributes().get("bundle-version"));

        assertEquals("org.example.a",
                exports.get(1).getAttributes().get("osgi.wiring.package"));
        assertEquals("org.example.d",
                exports.get(0).getAttributes().get("bundle-symbolic-name"));
        assertEquals(new Version(0, 0, 0),
                exports.get(0).getAttributes().get("bundle-version"));
    }

    @Test
    public void testPackageImports() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(
                new JarResource(new File(getClass()
                        .getResource("/testdata/05-import.jar").getPath())),
                caps, reqs);

        Requirement pkgImport = findReqs("osgi.wiring.package", reqs).get(0);
        assertEquals(
                "(&(osgi.wiring.package=org.example.a)(version>=1.0.0)(!(version>=2.0.0)))",
                pkgImport.getDirectives().get("filter"));
    }

    @Test
    public void testRequireBundle() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(new JarResource(new File(getClass()
                .getResource("/testdata/06-requirebundle.jar").getPath())),
                caps, reqs);

        List<Requirement> requires = findReqs("osgi.wiring.bundle", reqs);
        assertEquals(1, requires.size());
        assertEquals(
                "(&(osgi.wiring.bundle=org.example.a)(bundle-version>=3.0.0)(!(bundle-version>=4.0.0)))",
                requires.get(0).getDirectives().get("filter"));
    }

    @Test
    public void testPackageImportOptional() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(new JarResource(new File(getClass()
                .getResource("/testdata/07-optionalimport.jar").getPath())),
                caps, reqs);

        Requirement pkgImport = findReqs("osgi.wiring.package", reqs).get(0);
        assertEquals(
                "(&(osgi.wiring.package=org.example.a)(version>=1.0.0)(!(version>=2.0.0)))",
                pkgImport.getDirectives().get("filter"));
        assertEquals("optional", pkgImport.getDirectives().get("resolution"));
    }

    @Test
    public void testFragmentHost() throws Exception {
        BundleAnalyzer a = new BundleAnalyzer(new NullLogSvc());
        LinkedList<Capability> caps = new LinkedList<Capability>();
        LinkedList<Requirement> reqs = new LinkedList<Requirement>();

        a.analyzeResource(new JarResource(new File(getClass()
                .getResource("/testdata/08-fragmenthost.jar").getPath())), caps,
                reqs);

        Requirement req = findReqs("osgi.wiring.host", reqs).get(0);
        assertEquals(
                "(&(osgi.wiring.host=org.example.a)(bundle-version>=0.0.0))",
                req.getDirectives().get("filter"));
    }

}
