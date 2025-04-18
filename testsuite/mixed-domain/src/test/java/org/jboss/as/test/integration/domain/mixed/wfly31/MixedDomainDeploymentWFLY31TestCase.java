/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.domain.mixed.wfly31;

import org.jboss.as.test.integration.domain.mixed.MixedDomainDeploymentTest;
import org.jboss.as.test.integration.domain.mixed.Version;
import org.jboss.as.test.integration.domain.mixed.Version.AsVersion;
import org.junit.BeforeClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@Version(AsVersion.WFLY_31_0_0)
public class MixedDomainDeploymentWFLY31TestCase extends MixedDomainDeploymentTest {
    @BeforeClass
    public static void beforeClass() {
        MixedDomainWFLY31TestSuite.initializeDomain();
    }

    @Override
    protected boolean supportManagedExplodedDeployment() {
        return true;
    }
}
