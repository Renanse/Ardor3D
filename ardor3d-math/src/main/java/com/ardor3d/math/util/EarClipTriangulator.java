/**
 * Copyright (c) 2008-2019 Bird Dog Games, Inc.
 *
 * This file is part of Ardor3D.
 *
 * Ardor3D is free software: you can redistribute it and/or modify it
 * under the terms of its license which may be found in the accompanying
 * LICENSE file or at <https://git.io/fjRmv>.
 */

package com.ardor3d.math.util;

import java.util.ArrayList;
import java.util.List;

import com.ardor3d.math.MathUtils;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.type.ReadOnlyVector2;

/**
 * Ported to Java from JavaScript library mapbox/earcut v2.1.5
 * https://github.com/mapbox/earcut/blob/master/src/earcut.js
 *
 * mapbox/earcut license:
 *
 * ISC License
 *
 * Copyright (c) 2016, Mapbox
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose
 * with or without fee is hereby granted, provided that the above copyright notice
 * and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS
 * OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
 * THIS SOFTWARE.
 */

/**
 * @author Josh "renanse" Slack (ported to Ardor3D from mapbox/earcut)
 */
public class EarClipTriangulator {

    /**
     *
     * @param vertices
     * @param holeIndices
     * @return triangulated indices.
     */
    public static int[] triangulate(final Vector2[] vertices, final int... holeIndices) {
        final boolean hasHoles = holeIndices.length > 0;
        final int outerLen = hasHoles ? holeIndices[0] : vertices.length;
        EarClipNode outerNode = linkedList(vertices, 0, outerLen, true);
        final List<Integer> indices = new ArrayList<>();

        if (outerNode == null || outerNode.next == outerNode.prev) {
            System.err.println("Returning empty index array - unable to create valid node graph.");
            return new int[0];
        }

        if (hasHoles) {
            outerNode = eliminateHoles(vertices, holeIndices, outerNode);
        }

        double minX = 0.0, minY = 0.0, invSize = 0.0;

        // if the shape is not too simple, we'll use z-order curve hash later; calculate
        // polygon bbox
        if (vertices.length > 80) {
            double maxX, maxY, x, y;
            Vector2 vert = vertices[0];
            minX = maxX = vert.getX();
            minY = maxY = vert.getY();

            for (int i = 1; i < outerLen; i++) {
                vert = vertices[i];
                x = vert.getX();
                y = vert.getY();
                if (x < minX) {
                    minX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }

            // minX, minY and invSize are later used to transform coords into integers for
            // z-order calculation
            invSize = Math.max(maxX - minX, maxY - minY);
            invSize = Math.abs(invSize) > MathUtils.ZERO_TOLERANCE ? 1 / invSize : 0.0;
        }

        earcutLinked(outerNode, indices, minX, minY, invSize, 0);

        return indices.stream().mapToInt(Integer::intValue).toArray();
    }

    // create a circular doubly linked list from polygon points in the specified
    // winding order
    private static EarClipNode linkedList(final Vector2[] vertices, final int start, final int end,
            final boolean clockwise) {
        int i;
        EarClipNode last = null;

        if (clockwise == signedArea(vertices, start, end) > 0) {
            for (i = start; i < end; i++) {
                last = insertNode(i, vertices[i], last);
            }
        } else {
            for (i = end - 1; i >= start; i--) {
                last = insertNode(i, vertices[i], last);
            }
        }

        if (last != null && equals(last, last.next)) {
            removeNode(last);
            last = last.next;
        }

        return last;
    }

    // eliminate colinear or duplicate points
    private static EarClipNode filterPoints(final EarClipNode start, EarClipNode end) {
        if (start == null) {
            return start;
        }
        if (end == null) {
            end = start;
        }

        EarClipNode p = start;
        boolean again;

        do {
            again = false;

            if (!p.steiner && (equals(p, p.next) || area(p.prev, p, p.next) == 0)) {
                removeNode(p);
                p = end = p.prev;
                if (p == p.next) {
                    break;
                }
                again = true;

            } else {
                p = p.next;
            }
        } while (again || p != end);

        return end;
    }

    // main ear slicing loop which triangulates a polygon (given as a linked list)
    private static void earcutLinked(EarClipNode ear, final List<Integer> indices, final double minX, final double minY,
            final double invSize, final int pass) {
        if (ear == null) {
            return;
        }

        // interlink polygon nodes in z-order
        if (pass == 0 && invSize != 0.0) {
            indexCurve(ear, minX, minY, invSize);
        }

        EarClipNode stop = ear, prev, next;

        // iterate through ears, slicing them one by one
        while (ear.prev != ear.next) {
            prev = ear.prev;
            next = ear.next;

            if (invSize != 0.0 ? isEarHashed(ear, minX, minY, invSize) : isEar(ear)) {
                // cut off the triangle
                indices.add(prev.i);
                indices.add(ear.i);
                indices.add(next.i);

                removeNode(ear);

                // skipping the next vertex leads to less sliver triangles
                ear = next.next;
                stop = next.next;

                continue;
            }

            ear = next;

            // if we looped through the whole remaining polygon and can't find any more ears
            if (ear == stop) {
                switch (pass) {
                    case 0:
                        // try filtering points and slicing again
                        earcutLinked(filterPoints(ear, null), indices, minX, minY, invSize, 1);
                        break;
                    case 1:
                        // if this didn't work, try curing all small self-intersections locally
                        ear = cureLocalIntersections(ear, indices);
                        earcutLinked(ear, indices, minX, minY, invSize, 2);
                        break;
                    case 2:
                        // as a last resort, try splitting the remaining polygon into two
                        splitEarcut(ear, indices, minX, minY, invSize);
                        break;
                }

                break;
            }
        }
    }

    // check whether a polygon node forms a valid ear with adjacent nodes
    private static boolean isEar(final EarClipNode ear) {
        final EarClipNode a = ear.prev, b = ear, c = ear.next;

        if (area(a, b, c) >= 0) {
            return false; // reflex, can't be an ear
        }

        // now make sure we don't have other points inside the potential ear
        EarClipNode p = ear.next.next;

        while (p != ear.prev) {
            if (pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y) && area(p.prev, p, p.next) >= 0) {
                return false;
            }
            p = p.next;
        }

        return true;
    }

    private static boolean isEarHashed(final EarClipNode ear, final double minX, final double minY,
            final double invSize) {
        final EarClipNode a = ear.prev, b = ear, c = ear.next;

        if (area(a, b, c) >= 0) {
            return false; // reflex, can't be an ear
        }

        // triangle bbox; min & max are calculated like this for speed
        final double minTX = a.x < b.x ? a.x < c.x ? a.x : c.x : b.x < c.x ? b.x : c.x;
        final double minTY = a.y < b.y ? a.y < c.y ? a.y : c.y : b.y < c.y ? b.y : c.y;
        final double maxTX = a.x > b.x ? a.x > c.x ? a.x : c.x : b.x > c.x ? b.x : c.x;
        final double maxTY = a.y > b.y ? a.y > c.y ? a.y : c.y : b.y > c.y ? b.y : c.y;

        // z-order range for the current triangle bbox;
        final Integer minZ = zOrder(minTX, minTY, minX, minY, invSize),
                maxZ = zOrder(maxTX, maxTY, minX, minY, invSize);

        EarClipNode p = ear.prevZ, n = ear.nextZ;

        // look for points inside the triangle in both directions
        while (p != null && p.z >= minZ && n != null && n.z <= maxZ) {
            if (p != ear.prev && p != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y)
                    && area(p.prev, p, p.next) >= 0) {
                return false;
            }
            p = p.prevZ;

            if (n != ear.prev && n != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, n.x, n.y)
                    && area(n.prev, n, n.next) >= 0) {
                return false;
            }
            n = n.nextZ;
        }

        // look for remaining points in decreasing z-order
        while (p != null && p.z >= minZ) {
            if (p != ear.prev && p != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, p.x, p.y)
                    && area(p.prev, p, p.next) >= 0) {
                return false;
            }
            p = p.prevZ;
        }

        // look for remaining points in increasing z-order
        while (n != null && n.z <= maxZ) {
            if (n != ear.prev && n != ear.next && pointInTriangle(a.x, a.y, b.x, b.y, c.x, c.y, n.x, n.y)
                    && area(n.prev, n, n.next) >= 0) {
                return false;
            }
            n = n.nextZ;
        }

        return true;
    }

    // go through all polygon nodes and cure small local self-intersections
    private static EarClipNode cureLocalIntersections(EarClipNode start, final List<Integer> indices) {
        EarClipNode p = start;
        do {
            final EarClipNode a = p.prev, b = p.next.next;

            if (!equals(a, b) && intersects(a, p, p.next, b) && locallyInside(a, b) && locallyInside(b, a)) {

                indices.add(a.i);
                indices.add(p.i);
                indices.add(b.i);

                // remove two nodes involved
                removeNode(p);
                removeNode(p.next);

                p = start = b;
            }
            p = p.next;
        } while (p != start);

        return p;
    }

    // try splitting polygon into two and triangulate them independently
    private static void splitEarcut(final EarClipNode start, final List<Integer> indices, final double minX,
            final double minY, final double invSize) {
        // look for a valid diagonal that divides the polygon into two
        EarClipNode a = start;
        do {
            EarClipNode b = a.next.next;
            while (b != a.prev) {
                if (a.i != b.i && isValidDiagonal(a, b)) {
                    // split the polygon in two by the diagonal
                    EarClipNode c = splitPolygon(a, b);

                    // filter colinear points around the cuts
                    a = filterPoints(a, a.next);
                    c = filterPoints(c, c.next);

                    // run earcut on each half
                    earcutLinked(a, indices, minX, minY, invSize, 0);
                    earcutLinked(c, indices, minX, minY, invSize, 0);
                    return;
                }
                b = b.next;
            }
            a = a.next;
        } while (a != start);
    }

    // link every hole into the outer loop, producing a single-ring polygon without
    // holes
    private static EarClipNode eliminateHoles(final Vector2[] vertices, final int[] holeIndices,
            EarClipNode outerNode) {
        final List<EarClipNode> queue = new ArrayList<>();
        int i, len, start, end;
        EarClipNode list;

        for (i = 0, len = holeIndices.length; i < len; i++) {
            start = holeIndices[i];
            end = i < len - 1 ? holeIndices[i + 1] : vertices.length;
            list = linkedList(vertices, start, end, false);
            if (list == list.next) {
                list.steiner = true;
            }
            queue.add(getLeftmost(list));
        }

        queue.sort((final EarClipNode a, final EarClipNode b) -> {
            final double delta = a.x - b.x;
            return delta == 0.0 ? 0 : delta > 0 ? 1 : -1;
        });

        // process holes from left to right
        for (i = 0; i < queue.size(); i++) {
            eliminateHole(queue.get(i), outerNode);
            outerNode = filterPoints(outerNode, outerNode.next);
        }

        return outerNode;
    }

    // find a bridge between vertices that connects hole with an outer ring and and
    // link it
    private static void eliminateHole(final EarClipNode hole, EarClipNode outerNode) {
        outerNode = findHoleBridge(hole, outerNode);
        if (outerNode != null) {
            final EarClipNode b = splitPolygon(outerNode, hole);
            filterPoints(b, b.next);
        }
    }

    // David Eberly's algorithm for finding a bridge between hole and outer polygon
    private static EarClipNode findHoleBridge(final EarClipNode hole, final EarClipNode outerNode) {
        EarClipNode p = outerNode, m = null;
        final double hx = hole.x, hy = hole.y;
        double qx = Double.NEGATIVE_INFINITY;

        // find a segment intersected by a ray from the hole's leftmost point to the
        // left;
        // segment's endpoint with lesser x will be potential connection point
        do {
            if (hy <= p.y && hy >= p.next.y && p.next.y != p.y) {
                final double x = p.x + (hy - p.y) * (p.next.x - p.x) / (p.next.y - p.y);
                if (x <= hx && x > qx) {
                    qx = x;
                    if (x == hx) {
                        if (hy == p.y) {
                            return p;
                        }
                        if (hy == p.next.y) {
                            return p.next;
                        }
                    }
                    m = p.x < p.next.x ? p : p.next;
                }
            }
            p = p.next;
        } while (p != outerNode);

        if (m == null) {
            return null;
        }

        if (hx == qx) {
            return m.prev; // hole touches outer segment; pick lower endpoint
        }

        // look for points inside the triangle of hole point, segment intersection and
        // endpoint;
        // if there are no points found, we have a valid connection; otherwise choose
        // the point of the minimum angle with the ray as connection point

        final EarClipNode stop = m;
        final double mx = m.x, my = m.y;
        double tanMin = Double.POSITIVE_INFINITY, tan;

        p = m.next;

        while (p != stop) {
            if (hx >= p.x && p.x >= mx && hx != p.x
                    && pointInTriangle(hy < my ? hx : qx, hy, mx, my, hy < my ? qx : hx, hy, p.x, p.y)) {

                tan = Math.abs(hy - p.y) / (hx - p.x); // tangential

                if ((tan < tanMin || tan == tanMin && p.x > m.x) && locallyInside(p, hole)) {
                    m = p;
                    tanMin = tan;
                }
            }

            p = p.next;
        }

        return m;
    }

    // interlink polygon nodes in z-order
    private static void indexCurve(final EarClipNode start, final double minX, final double minY,
            final double invSize) {
        EarClipNode p = start;
        do {
            if (p.z == null) {
                p.z = zOrder(p.x, p.y, minX, minY, invSize);
            }
            p.prevZ = p.prev;
            p.nextZ = p.next;
            p = p.next;
        } while (p != start);

        p.prevZ.nextZ = null;
        p.prevZ = null;

        sortLinked(p);
    }

    // Simon Tatham's linked list merge sort algorithm
    // http://www.chiark.greenend.org.uk/~sgtatham/algorithms/listsort.html
    private static EarClipNode sortLinked(EarClipNode list) {
        int i;
        EarClipNode p, q, e, tail;
        int inSize = 1, numMerges, pSize, qSize;

        do {
            p = list;
            list = null;
            tail = null;
            numMerges = 0;

            while (p != null) {
                numMerges++;
                q = p;
                pSize = 0;
                for (i = 0; i < inSize; i++) {
                    pSize++;
                    q = q.nextZ;
                    if (q == null) {
                        break;
                    }
                }
                qSize = inSize;

                while (pSize > 0 || qSize > 0 && q != null) {

                    if (pSize != 0 && (qSize == 0 || q == null || p.z <= q.z)) {
                        e = p;
                        p = p.nextZ;
                        pSize--;
                    } else {
                        e = q;
                        q = q.nextZ;
                        qSize--;
                    }

                    if (tail != null) {
                        tail.nextZ = e;
                    } else {
                        list = e;
                    }

                    e.prevZ = tail;
                    tail = e;
                }

                p = q;
            }

            tail.nextZ = null;
            inSize *= 2;

        } while (numMerges > 1);

        return list;
    }

    // z-order of a Vertex given coords and size of the data bounding box
    private static Integer zOrder(final double vertX, final double vertY, final double minX, final double minY,
            final double invSize) {
        // coords are transformed into non-negative 15-bit integer range
        int x = (int) (32767 * (vertX - minX) * invSize);
        int y = (int) (32767 * (vertY - minY) * invSize);

        x = (x | x << 8) & 0x00FF00FF;
        x = (x | x << 4) & 0x0F0F0F0F;
        x = (x | x << 2) & 0x33333333;
        x = (x | x << 1) & 0x55555555;

        y = (y | y << 8) & 0x00FF00FF;
        y = (y | y << 4) & 0x0F0F0F0F;
        y = (y | y << 2) & 0x33333333;
        y = (y | y << 1) & 0x55555555;

        return x | y << 1;
    }

    // find the leftmost node of a polygon ring
    private static EarClipNode getLeftmost(final EarClipNode start) {
        EarClipNode p = start, leftmost = start;
        do {
            if (p.x < leftmost.x || p.x == leftmost.x && p.y < leftmost.y) {
                leftmost = p;
            }
            p = p.next;
        } while (p != start);

        return leftmost;
    }

    // check if a point lies within a triangle
    private static boolean pointInTriangle(final double ax, final double ay, final double bx, final double by,
            final double cx, final double cy, final double px, final double py) {
        final double apX = ax - px;
        final double apY = ay - py;
        final double bpX = bx - px;
        final double bpY = by - py;
        final double cpX = cx - px;
        final double cpY = cy - py;
        return cpX * apY - apX * cpY >= 0 && //
                apX * bpY - bpX * apY >= 0 && //
                bpX * cpY - cpX * bpY >= 0;
    }

    // check if a diagonal between two polygon nodes is valid (lies in polygon
    // interior)
    private static boolean isValidDiagonal(final EarClipNode a, final EarClipNode b) {
        return a.next.i != b.i && a.prev.i != b.i && !intersectsPolygon(a, b) && locallyInside(a, b)
                && locallyInside(b, a) && middleInside(a, b);
    }

    // signed area of a triangle
    private static double area(final EarClipNode p, final EarClipNode q, final EarClipNode r) {
        return (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
    }

    // check if two points are equal
    private static boolean equals(final EarClipNode p1, final EarClipNode p2) {
        return p1.x == p2.x && p1.y == p2.y;
    }

    // check if two segments intersect
    private static boolean intersects(final EarClipNode p1, final EarClipNode q1, final EarClipNode p2,
            final EarClipNode q2) {
        if (equals(p1, p2) && equals(q1, q2) || equals(p1, q2) && equals(p2, q1)) {
            return true;
        }
        return area(p1, q1, p2) > 0 != area(p1, q1, q2) > 0 && area(p2, q2, p1) > 0 != area(p2, q2, q1) > 0;
    }

    // check if a polygon diagonal intersects any polygon segments
    private static boolean intersectsPolygon(final EarClipNode a, final EarClipNode b) {
        EarClipNode p = a;
        do {
            if (p.i != a.i && p.next.i != a.i && p.i != b.i && p.next.i != b.i && intersects(p, p.next, a, b)) {
                return true;
            }
            p = p.next;
        } while (p != a);

        return false;
    }

    // check if a polygon diagonal is locally inside the polygon
    private static boolean locallyInside(final EarClipNode a, final EarClipNode b) {
        return area(a.prev, a, a.next) < 0 ? area(a, b, a.next) >= 0 && area(a, a.prev, b) >= 0
                : area(a, b, a.prev) < 0 || area(a, a.next, b) < 0;
    }

    // check if the middle point of a polygon diagonal is inside the polygon
    private static boolean middleInside(final EarClipNode a, final EarClipNode b) {
        EarClipNode p = a;
        boolean inside = false;
        final double px = (a.x + b.x) / 2.0;
        final double py = (a.y + b.y) / 2.0;

        do {
            if (p.y > py != p.next.y > py && p.next.y != p.y
                    && px < (p.next.x - p.x) * (py - p.y) / (p.next.y - p.y) + p.x) {
                inside = !inside;
            }
            p = p.next;

        } while (p != a);

        return inside;
    }

    // link two polygon vertices with a bridge; if the vertices belong to the same
    // ring, it splits polygon into two; if one belongs to the outer ring and
    // another to a hole, it merges it into a single ring
    private static EarClipNode splitPolygon(final EarClipNode a, final EarClipNode b) {
        final EarClipNode a2 = new EarClipNode(a.i, a.x, a.y), b2 = new EarClipNode(b.i, b.x, b.y), an = a.next,
                bp = b.prev;

        a.next = b;
        b.prev = a;

        a2.next = an;
        an.prev = a2;

        b2.next = a2;
        a2.prev = b2;

        bp.next = b2;
        b2.prev = bp;

        return b2;
    }

    // create a node and optionally link it with previous one (in a circular doubly
    // linked list)
    private static EarClipNode insertNode(final int i, final ReadOnlyVector2 vert, final EarClipNode last) {
        final EarClipNode p = new EarClipNode(i, vert.getX(), vert.getY());

        if (last == null) {
            p.prev = p;
            p.next = p;

        } else {
            p.next = last.next;
            p.prev = last;
            last.next.prev = p;
            last.next = p;
        }

        return p;
    }

    private static void removeNode(final EarClipNode p) {
        p.next.prev = p.prev;
        p.prev.next = p.next;

        if (p.prevZ != null) {
            p.prevZ.nextZ = p.nextZ;
        }
        if (p.nextZ != null) {
            p.nextZ.prevZ = p.prevZ;
        }
    }

    static class EarClipNode {
        // vertex index in coordinates array
        final int i;

        // vertex coordinates
        final double x, y;

        // previous and next vertex nodes in a polygon ring
        EarClipNode next, prev;

        // z-order curve value
        Integer z;

        // previous and next nodes in z-order
        EarClipNode nextZ, prevZ;

        // indicates whether this is a steiner point
        boolean steiner = false;

        public EarClipNode(final int i, final double x, final double y) {
            this.i = i;
            this.x = x;
            this.y = y;

            prev = null;
            next = null;

            z = null;

            prevZ = null;
            nextZ = null;

            steiner = false;
        }
    }

    private static double signedArea(final Vector2[] vertices, final int start, final int end) {
        double sum = 0;
        for (int i = start, j = end - 1; i < end; i++) {
            final Vector2 pointA = vertices[i];
            final Vector2 pointB = vertices[j];
            sum += (pointB.getX() - pointA.getX()) * (pointB.getY() + pointA.getY());
            j = i;
        }

        return sum;
    }

}
