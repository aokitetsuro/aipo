dojo._xdResourceLoaded({
depends: [["provide", "dojox.gfx3d.vector"]],
defineResource: function(dojo){if(!dojo._hasResource["dojox.gfx3d.vector"]){ //_hasResource checks added by build. Do not use _hasResource directly in your code.
dojo._hasResource["dojox.gfx3d.vector"] = true;
dojo.provide("dojox.gfx3d.vector");

dojo.mixin(dojox.gfx3d.vector, {
	sum: function(){
		// summary: sum of the vectors
		var v = {x: 0, y: 0, z:0};
		dojo.forEach(arguments, function(item){ v.x += item.x; v.y += item.y; v.z += item.z; });
		return v;
	},

	center: function(){
		// summary: center of the vectors
		var l = arguments.length;
		if(l == 0){
			return {x: 0, y: 0, z: 0};
		} 
		var v = dojox.gfx3d.vector.sum(arguments);
		return {x: v.x/l, y: v.y/l, z: v.z/l};
	},

	substract: function(/* Pointer */a, /* Pointer */b){
		return  {x: a.x - b.x, y: a.y - b.y, z: a.z - b.z};
	},

	_crossProduct: function(x, y, z, u, v, w){
		// summary: applies a cross product of two vectorss, (x, y, z) and (u, v, w)
		// x: Number: an x coordinate of a point
		// y: Number: a y coordinate of a point
		// z: Number: a z coordinate of a point
		// u: Number: an x coordinate of a point
		// v: Number: a y coordinate of a point
		// w: Number: a z coordinate of a point
		return {x: y * w - z * v, y: z * u - x * w, z: x * v - y * u}; // Object
	},

	crossProduct: function(/* Number||Point */ a, /* Number||Point */ b, /* Number, optional */ c, /* Number, optional */ d, /* Number, optional */ e, /* Number, optional */ f){
		// summary: applies a matrix to a point
		// matrix: dojox.gfx3d.matrix.Matrix3D: a 3D matrix object to be applied
		// a: Number: an x coordinate of a point
		// b: Number: a y coordinate of a point
		// c: Number: a z coordinate of a point
		// d: Number: an x coordinate of a point
		// e: Number: a y coordinate of a point
		// f: Number: a z coordinate of a point
		if(arguments.length == 6 && dojo.every(arguments, function(item){ return typeof item == "number"; })){
			return dojox.gfx3d.vector._crossProduct(a, b, c, d, e, f); // Object
		}
		// branch
		// a: Object: a point
		// b: Object: a point
		// c: null
		// d: null
		// e: null
		// f: null
		return dojox.gfx3d.vector._crossProduct(a.x, a.y, a.z, b.x, b.y, b.z); // Object
	},

	_dotProduct: function(x, y, z, u, v, w){
		// summary: applies a cross product of two vectorss, (x, y, z) and (u, v, w)
		// x: Number: an x coordinate of a point
		// y: Number: a y coordinate of a point
		// z: Number: a z coordinate of a point
		// u: Number: an x coordinate of a point
		// v: Number: a y coordinate of a point
		// w: Number: a z coordinate of a point
		return x * u + y * v + z * w; // Number
	},
	dotProduct: function(/* Number||Point */ a, /* Number||Point */ b, /* Number, optional */ c, /* Number, optional */ d, /* Number, optional */ e, /* Number, optional */ f){
		// summary: applies a matrix to a point
		// matrix: dojox.gfx3d.matrix.Matrix3D: a 3D matrix object to be applied
		// a: Number: an x coordinate of a point
		// b: Number: a y coordinate of a point
		// c: Number: a z coordinate of a point
		// d: Number: an x coordinate of a point
		// e: Number: a y coordinate of a point
		// f: Number: a z coordinate of a point
		if(arguments.length == 6 && dojo.every(arguments, function(item){ return typeof item == "number"; })){
			return dojox.gfx3d.vector._dotProduct(a, b, c, d, e, f); // Object
		}
		// branch
		// a: Object: a point
		// b: Object: a point
		// c: null
		// d: null
		// e: null
		// f: null
		return dojox.gfx3d.vector._dotProduct(a.x, a.y, a.z, b.x, b.y, b.z); // Object
	},

	normalize: function(/* Point||Array*/ a, /* Point */ b, /* Point */ c){
		// summary: find the normal of the implicit surface
		// a: Object: a point
		// b: Object: a point
		// c: Object: a point
		var l, m, n; 
		if(a instanceof Array){
			l = a[0]; m = a[1]; n = a[2];
		}else{
			l = a; m = b; n = c;
		}

		var u = dojox.gfx3d.vector.substract(m, l);
		var v = dojox.gfx3d.vector.substract(n, l);
		return dojox.gfx3d.vector.crossProduct(u, v);
	}
});

}

}});