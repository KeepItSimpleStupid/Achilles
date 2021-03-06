/*
 * Copyright (C) 2012-2016 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.archinnov.achilles.embedded;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

@RunWith(MockitoJUnitRunner.class)
public class CassandraEmbeddedServerBuilderTest {

    @Test
    public void should_start_new_embedded_server() throws Exception {
        //Given
        String keyspace = RandomStringUtils.randomAlphabetic(9);
        final Session session = CassandraEmbeddedServerBuilder.builder()
                .withKeyspaceName(keyspace)
                .buildNativeSession();

        //Then
        assertThat(session).isNotNull();
        final Row one = session.execute("SELECT * FROM system.local LIMIT 1").one();
        assertThat(one).isNotNull();
    }

}