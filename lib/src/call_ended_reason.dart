/// Reasons for a call to end, as reported by the [ConnectycubeFlutterCallKit.reportCallFinished].

enum CallEndedReason {
  /// An error occurred while trying to service the call.
  failed,

  /// The remote party explicitly ended the call.
  remoteEnded,

  /// The call never started connecting and was never
  /// explicitly ended (e.g. outgoing/incoming call timeout).
  unanswered,

  /// The call was answered on another device.
  answeredElsewhere,

  /// The call was declined on another device.
  declinedElsewhere
}
