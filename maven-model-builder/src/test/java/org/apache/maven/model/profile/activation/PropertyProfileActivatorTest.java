package org.apache.maven.model.profile.activation;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.ActivationProperty;
import org.apache.maven.model.Profile;

/**
 * Tests {@link PropertyProfileActivator}.
 *
 * @author Benjamin Bentmann
 */
public class PropertyProfileActivatorTest
    extends AbstractProfileActivatorTest<PropertyProfileActivator>
{

    public PropertyProfileActivatorTest()
    {
        super( PropertyProfileActivator.class );
    }

    private Profile newProfile( String key, String value )
    {
        ActivationProperty ap = new ActivationProperty();
        ap.setName( key );
        ap.setValue( value );

        Activation a = new Activation();
        a.setProperty( ap );

        Profile p = new Profile();
        p.setActivation( a );

        return p;
    }

    private Properties newProperties( String key, String value )
    {
        Properties props = new Properties();
        props.setProperty( key, value );
        return props;
    }

    public void testNullSafe()
        throws Exception
    {
        Profile p = new Profile();

        assertActivation( false, p, newContext( null, null, null ) );

        p.setActivation( new Activation() );

        assertActivation( false, p, newContext( null, null, null ) );
    }

    public void testWithNameOnly_UserProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", null );

        assertActivation( true, profile, newContext( newProperties( "prop", "value" ), null, null ) );

        assertActivation( false, profile, newContext( newProperties( "prop", "" ), null, null ) );

        assertActivation( false, profile, newContext( newProperties( "other", "value" ), null, null ) );
    }

    public void testWithNameOnly_SystemProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", null );

        assertActivation( true, profile, newContext( null, newProperties( "prop", "value" ), null ) );

        assertActivation( false, profile, newContext( null, newProperties( "prop", "" ), null ) );

        assertActivation( false, profile, newContext (null, newProperties( "other", "value" ), null ) );
    }

    public void testWithNameOnly_ProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", null );

        assertActivation( true, profile, newContext( null, null, newProperties( "prop", "value" ) ) );

        assertActivation( false, profile, newContext( null, null, newProperties( "prop", "" ) ) );

        assertActivation( false, profile, newContext( null, null, newProperties( "other", "value" ) ) );
    }

    public void testWithNegatedNameOnly_UserProperty()
        throws Exception
    {
        Profile profile = newProfile( "!prop", null );

        assertActivation( false, profile, newContext( newProperties( "prop", "value" ), null, null ) );

        assertActivation( true, profile, newContext( newProperties( "prop", "" ), null, null ) );

        assertActivation( true, profile, newContext( newProperties( "other", "value" ), null, null ) );
    }

    public void testWithNegatedNameOnly_SystemProperty()
        throws Exception
    {
        Profile profile = newProfile( "!prop", null );

        assertActivation( false, profile, newContext( null, newProperties( "prop", "value" ), null ) );

        assertActivation( true, profile, newContext( null, newProperties( "prop", "" ), null ) );

        assertActivation( true, profile, newContext( null, newProperties( "other", "value" ), null ) );
    }

    public void testWithNegatedNameOnly_ProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "!prop", null );

        assertActivation( false, profile, newContext( null, null, newProperties( "prop", "value" ) ) );

        assertActivation( true, profile, newContext( null, null, newProperties( "prop", "" ) ) );

        assertActivation( true, profile, newContext( null, null, newProperties( "other", "value" ) ) );
    }

    public void testWithValue_UserProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", "value" );

        assertActivation( true, profile, newContext( newProperties( "prop", "value" ), null, null ) );

        assertActivation( false, profile, newContext( newProperties( "prop", "other" ), null, null ) );

        assertActivation( false, profile, newContext( newProperties( "prop", "" ), null, null ) );
    }

    public void testWithValue_SystemProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", "value" );

        assertActivation( true, profile, newContext( null, newProperties( "prop", "value" ), null ) );

        assertActivation( false, profile, newContext( null, newProperties( "prop", "other" ), null ) );

        assertActivation( false, profile, newContext( null, newProperties( "other", "" ), null ) );
    }

    public void testWithValue_ProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "value" );

        assertActivation( true, profile, newContext( null, null, newProperties("prop", "value") ) );

        assertActivation( false, profile, newContext( null, null, newProperties( "prop", "other" ) ) );

        assertActivation( false, profile, newContext( null, null, newProperties( "other", "" ) ) );
    }

    public void testWithNegatedValue_UserProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", "!value" );

        assertActivation( false, profile, newContext( newProperties( "prop", "value" ), null, null ) );

        assertActivation( true, profile, newContext( newProperties( "prop", "other" ), null, null ) );

        assertActivation( true, profile, newContext( newProperties( "prop", "" ), null, null ) );
    }

    public void testWithNegatedValue_SystemProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", "!value" );

        assertActivation( false, profile, newContext( null, newProperties( "prop", "value" ), null ) );

        assertActivation( true, profile, newContext( null, newProperties( "prop", "other" ), null ) );

        assertActivation( true, profile, newContext( null, newProperties( "other", "" ), null ) );
    }

    public void testWithNegatedValue_ProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "!value" );

        assertActivation( false, profile, newContext( null, null, newProperties( "prop", "value" ) ) );

        assertActivation( true, profile, newContext( null, null, newProperties( "prop", "other" ) ) );

        assertActivation( true, profile, newContext( null, null, newProperties( "other", "" ) ) );
    }

    public void testWithRegexValue_UserProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "~.*lue" );

        assertActivation( true, profile, newContext( newProperties( "prop", "value" ), null, null ) );

        assertActivation( false, profile, newContext( newProperties( "prop", "other" ), null, null ) );

        assertActivation( false, profile, newContext( newProperties( "prop", "" ), null, null ) );
    }

    public void testWithRegexValue_SystemProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "~.*lue" );

        assertActivation( true, profile, newContext( null, newProperties( "prop", "value" ), null ) );

        assertActivation( false, profile, newContext( null, newProperties( "prop", "other" ), null ) );

        assertActivation( false, profile, newContext( null, newProperties( "other", "" ), null ) );
    }

    public void testWithRegexValue_ProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "~.*lue" );

        assertActivation( true, profile, newContext( null, null, newProperties( "prop", "value" ) ) );

        assertActivation( false, profile, newContext( null, null, newProperties( "prop", "other" ) ) );

        assertActivation( false, profile, newContext( null, null, newProperties( "other", "" ) ) );
    }

    public void testWithNegatedRegexValue_UserProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "!~.*lue" );

        assertActivation( false, profile, newContext( newProperties( "prop", "value" ), null, null ) );

        assertActivation( true, profile, newContext( newProperties( "prop", "other" ), null, null ) );

        assertActivation( true, profile, newContext( newProperties( "prop", "" ), null, null ) );
    }

    public void testWithNegatedRegexValue_SystemProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "!~.*lue" );

        assertActivation( false, profile, newContext( null, newProperties( "prop", "value" ), null ) );

        assertActivation( true, profile, newContext( null, newProperties( "prop", "other" ), null ) );

        assertActivation( true, profile, newContext( null, newProperties( "other", "" ), null ) );
    }

    public void testWithNegatedRegexValue_ProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "!~.*lue" );

        assertActivation( false, profile, newContext( null, null, newProperties( "prop", "value" ) ) );

        assertActivation( true, profile, newContext( null, null, newProperties( "prop", "other" ) ) );

        assertActivation( true, profile, newContext( null, null, newProperties( "other", "" ) ) );
    }

    public void testWithValue_UserPropertyDominantOverSystemProperty()
        throws Exception
    {
        Profile profile = newProfile( "prop", "value" );

        Properties props1 = newProperties( "prop", "value" );
        Properties props2 = newProperties( "prop", "other" );

        assertActivation( true, profile, newContext( props1, props2, null ) );

        assertActivation( false, profile, newContext( props2, props1, null ) );
    }

    public void testWithValue_UserPropertyDominantOverProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "value" );

        Properties props1 = newProperties( "prop", "value" );
        Properties props2 = newProperties( "prop", "other" );

        assertActivation( true, profile, newContext( props1, null, props2 ) );

        assertActivation( false, profile, newContext( props2, null, props1 ) );
    }

    public void testWithValue_SystemPropertyDominantOverProjectProperty()
            throws Exception
    {
        Profile profile = newProfile( "prop", "value" );

        Properties props1 = newProperties( "prop", "value" );
        Properties props2 = newProperties( "prop", "other" );

        assertActivation( true, profile, newContext( null, props1, props2 ) );

        assertActivation( false, profile, newContext( null, props2, props1 ) );
    }
}
