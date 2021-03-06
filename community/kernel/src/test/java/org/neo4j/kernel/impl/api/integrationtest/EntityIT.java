/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api.integrationtest;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.neo4j.kernel.api.DataWriteOperations;
import org.neo4j.kernel.impl.util.PrimitiveLongIterator;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.*;
import static org.neo4j.graphdb.Direction.BOTH;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.Iterables.toList;

public class EntityIT extends KernelIntegrationTest
{
    @Test
    public void shouldListRelationshipsInCurrentAndSubsequentTx() throws Exception
    {
        // given
        long refNode, fromRefToOther1, fromRefToOther2, fromOtherToRef, fromRefToRef, fromRefToThird;
        int relType1, relType2;
        {
            DataWriteOperations statement = dataWriteOperationsInNewTransaction();

            relType1 = statement.relationshipTypeGetOrCreateForName( "Type1" );
            relType2 = statement.relationshipTypeGetOrCreateForName( "Type2" );

            refNode = statement.nodeCreate();
            long otherNode = statement.nodeCreate();
            fromRefToOther1 = statement.relationshipCreate( relType1, refNode, otherNode );
            fromRefToOther2 = statement.relationshipCreate( relType2, refNode, otherNode );
            fromOtherToRef = statement.relationshipCreate( relType1, otherNode, refNode );
            fromRefToRef = statement.relationshipCreate( relType2, refNode, refNode );
            fromRefToThird = statement.relationshipCreate( relType2, refNode, statement.nodeCreate() );

            // when & then
            assertRels( statement.relationshipsGetFromNode( refNode, BOTH ),
                        fromRefToOther1, fromRefToOther2, fromRefToRef, fromRefToThird, fromOtherToRef);

            assertRels( statement.relationshipsGetFromNode( refNode, BOTH, relType1 ),
                    fromRefToOther1, fromOtherToRef);

            assertRels( statement.relationshipsGetFromNode( refNode, BOTH, new int[]{relType1, relType2} ),
                    fromRefToOther1, fromRefToOther2, fromRefToRef, fromRefToThird, fromOtherToRef);


            assertRels( statement.relationshipsGetFromNode( refNode, INCOMING ), fromOtherToRef );

            assertRels( statement.relationshipsGetFromNode( refNode, INCOMING, relType1 ) /* none */);

            assertRels( statement.relationshipsGetFromNode( refNode, OUTGOING, new int[]{relType1, relType2} ),
                    fromRefToOther1, fromRefToOther2, fromRefToThird, fromRefToRef);

            // when
            commit();
        }
        {
            DataWriteOperations statement = dataWriteOperationsInNewTransaction();

            // when & then
            assertRels( statement.relationshipsGetFromNode( refNode, BOTH ),
                    fromRefToOther1, fromRefToOther2, fromRefToRef, fromRefToThird, fromOtherToRef);

            assertRels( statement.relationshipsGetFromNode( refNode, BOTH, relType1 ),
                    fromRefToOther1, fromOtherToRef);

            assertRels( statement.relationshipsGetFromNode( refNode, BOTH, new int[]{relType1, relType2} ),
                    fromRefToOther1, fromRefToOther2, fromRefToRef, fromRefToThird, fromOtherToRef);

            assertRels( statement.relationshipsGetFromNode( refNode, INCOMING ), fromOtherToRef );

            assertRels( statement.relationshipsGetFromNode( refNode, INCOMING, relType1 ) /* none */);

            assertRels( statement.relationshipsGetFromNode( refNode, OUTGOING, new int[]{relType1, relType2} ),
                    fromRefToOther1, fromRefToOther2, fromRefToThird, fromRefToRef);
        }
    }

    private void assertRels( PrimitiveLongIterator it, long ... rels )
    {
        List<Matcher<? super Iterable<Long>>> all = new ArrayList<>(rels.length);
        for (long element : rels) {
            all.add(hasItem(element));
        }

        List<Long> list = toList( it );
        assertThat( list, allOf(all));
    }
}