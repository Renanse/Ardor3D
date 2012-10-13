importPackage(Packages.com.ardor3d.example.pipeline);

// set up walk/run as our base layer as separate states.
var walkState = {
	"name" : "walk_anim",
	"clip" : "skeleton.walk",
	"transitions" : {
	    "run" : [
             "-", "-", // start/end window
	         "syncfade", // type
	         "run_anim", // target
	         0.5, // fade time
	         "Linear" // type
	         ]
	},
	"endTransition" : [
         "-", "-", // start/end window
         "immediate", // type
         "walk_anim" // target
         ]
};

// add to default layer
_steadyState(walkState);

var runState = {
	"name" : "run_anim",
	"clip" : "skeleton.run",
	"transitions" : {
	    "walk" : [
             "-", "-", // start/end window
	         "syncfade", // type
	         "walk_anim", // target
	         0.75, // fade time
	         "Linear" // type
	         ]
	},
	"endTransition" : [
             "-", "-", // start/end window
	         "immediate", // type
	         "run_anim" // target
	         ]
};

// add to default layer
_steadyState(runState);

/**
 * Our Punch Layer
 */
var punchLayerInfo = {
	"name" : "punch",
	"blendType" : "lerp",
	"blendWeight" : 1.0,
	"blendKey" : "punch_blend"
};

//add new "punch" layer
_animationLayer(punchLayerInfo);

// set up punching state. Will not always be playing.
var punchState = {
	"name" : "punch_right",
	"layer" : "punch",
	"tree" : {
		"inclusiveClip" : {
			"name" : "skeleton.punch",
			joints : [11, 12, 13, 14, 15],
			channels : ["punch_fire"],
			"active" : false
		}
	}
};

// add to "punch" layer
_steadyState(punchState);

// set up trigger info
var triggerChannelInfo = {
	"clip" : "skeleton.punch", // clip to add trigger channel to
	"triggerChannel" : {  // information about the channel
		"name" : "punch_fire", // name of channel
		"times" : [0.0, 0.5, 0.75],
		"keys" : [null, "fist_fire", null]
	}
};

// add our punch trigger channel
_addTriggerChannel(triggerChannelInfo);

// add our callbacks
_addTriggerCallback("fist_fire", new FireballTrigger());
