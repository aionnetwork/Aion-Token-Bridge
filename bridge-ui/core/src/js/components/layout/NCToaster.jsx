import {Position, Toaster} from "@blueprintjs/core";

/** Singleton toaster instance. Create separate instances for different options. */
const NCToaster = Toaster.create({
  className: "nc-toaster",
  position: Position.BOTTOM_RIGHT,
});

export default NCToaster;