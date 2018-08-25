/*
* Copyright [2016-2018] [George Papadakis (gpapadis@yahoo.gr)]
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
 */
package org.scify.jedai.blockprocessing.comparisoncleaning;

import com.esotericsoftware.minlog.Log;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.utilities.enumerations.WeightingScheme;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.List;
import java.util.Random;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;

/**
 *
 * @author gap2
 */
public class CanopyClustering extends CardinalityNodePruning {

    protected final double inclusiveThreshold;
    protected final double exclusiveThreshold;
    protected TIntSet excludedEntities;

    public CanopyClustering() {
        this(0.5, 0.75, WeightingScheme.ARCS);
    }

    public CanopyClustering(double inThr, double outThr) {
        this(inThr, outThr, WeightingScheme.ARCS);
    }

    public CanopyClustering(double inThr, double outThr, WeightingScheme scheme) {
        super(scheme);
        nodeCentric = true;
        exclusiveThreshold = outThr;
        inclusiveThreshold = inThr;
        if (exclusiveThreshold < inclusiveThreshold) {
            Log.error(getMethodName(), "The " + getParameterName(1) + " cannot be smaller than the " + getParameterName(0));
            System.exit(-1);
        }
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + inclusiveThreshold + ",\t"
                + getParameterName(1) + "=" + exclusiveThreshold;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": a Meta-blocking method that retains for every entity, the comparisons "
                + "(i.e., edges in the blocking graph) that exceed the " + getParameterName(0) + "."
                + "Comparisons exceeding the " + getParameterName(1) + " are not considered as candidate matches "
                + "for any other node.";
    }

    @Override
    public String getMethodName() {
        return "Canopy Clustering";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.Double");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "0.5");
        obj1.put("minValue", "0.1");
        obj1.put("maxValue", "0.9");
        obj1.put("stepValue", "0.05");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Double");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "0.75");
        obj2.put("minValue", "0.2");
        obj2.put("maxValue", "0.95");
        obj2.put("stepValue", "0.05");
        obj2.put("description", getParameterDescription(1));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " defines the minimum similarity of a retained comparison/edge per entity/node.";
            case 1:
                return "The " + getParameterName(1) + " defines the similarity above which a node is removed from the "
                        + "blocking graph so that it is not considered as a candidate match for any other node.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "Inclusive Threshold";
            case 1:
                return "Exclusive Threshold";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    protected List<AbstractBlock> pruneEdges() {
        final TIntList entityIds = new TIntArrayList();
        for (int i = 0; i < noOfEntities; i++) {
            entityIds.add(i);
        }
        entityIds.shuffle(new Random());
        final TIntIterator iterator = entityIds.iterator();

        excludedEntities = new TIntHashSet();
        nearestEntities = new TIntSet[noOfEntities];
        if (weightingScheme.equals(WeightingScheme.ARCS)) {
            while (iterator.hasNext()) {
                int currentId = iterator.next();
                processArcsEntity(currentId);
                verifyValidEntities(currentId);
            }
        } else {
            while (iterator.hasNext()) {
                int currentId = iterator.next();
                processEntity(currentId);
                verifyValidEntities(currentId);
            }
        }

        return retainValidComparisons();
    }

    @Override
    protected void setThreshold() {
    }

    @Override
    protected void verifyValidEntities(int entityId) {
        nearestEntities[entityId] = new TIntHashSet();
        for (TIntIterator tIterator = validEntities.iterator(); tIterator.hasNext();) {
            int neighborId = tIterator.next();
            if (excludedEntities.contains(neighborId)) {
                System.out.println("Excluded!!!");
                continue;
            }

            double weight = getWeight(entityId, neighborId);
            if (inclusiveThreshold < weight) {
                if (exclusiveThreshold < weight) {
                    System.out.println(weight);
                    excludedEntities.add(neighborId);
                }
                nearestEntities[entityId].add(neighborId);
            }
        }
    }
}
